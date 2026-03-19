package daycare.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import daycare.model.Animal;
import daycare.model.Owner;
import daycare.service.DaycareResult;
import daycare.service.DaycareService;
import daycare.storage.JsonOwnerStorage;
import daycare.util.SimpleJson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class DaycareApiServer {
    private static final int DEFAULT_PORT = 8080;

    private final DaycareService service;

    public DaycareApiServer(DaycareService service) {
        this.service = service;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        DaycareApiServer apiServer = new DaycareApiServer(new DaycareService(new JsonOwnerStorage()));
        apiServer.start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::handleApiRequest);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        System.out.println("Daycare API kör på http://localhost:" + port + "/api");
        System.out.println("JSON-data lagras via OwnerStorage-konfigurationen.");
    }

    private void handleApiRequest(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/health".equals(path) && "GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, response(true, "API:n är igång.", healthData()));
                return;
            }
            if ("/api/owners".equals(path) && "GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, response(true, null, ownersData(service.getAllOwners())));
                return;
            }
            if ("/api/owners".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreateOwner(exchange);
                return;
            }
            if (path.startsWith("/api/owners/")) {
                handleOwnerRoutes(exchange, path, method);
                return;
            }
            if ("/api/check-in".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCheckIn(exchange);
                return;
            }
            if ("/api/check-out".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCheckOut(exchange);
                return;
            }
            if ("/api/transfer".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleTransfer(exchange);
                return;
            }
            if ("/api/checked-in".equals(path) && "GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, response(true, null, checkedInAnimalsData()));
                return;
            }

            sendJson(exchange, 404, response(false, "Endpoint hittades inte.", null));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, response(false, e.getMessage(), null));
        } catch (Exception e) {
            sendJson(exchange, 500, response(false, "Internt serverfel: " + e.getMessage(), null));
        }
    }

    private void handleCreateOwner(HttpExchange exchange) throws IOException {
        Map<String, Object> request = readJsonObject(exchange);
        String name = requireString(request, "name");
        String phone = requireString(request, "phone");

        DaycareResult<Owner> result = service.registerOwner(name, phone);
        int status = result.isSuccess() ? 201 : statusForResult(result);
        Object data = result.isSuccess() ? ownerData(result.getValue()) : null;
        sendJson(exchange, status, response(result.isSuccess(), result.getMessage(), data));
    }

    private void handleOwnerRoutes(HttpExchange exchange, String path, String method) throws IOException {
        String tail = path.substring("/api/owners/".length());
        if (tail.endsWith("/animals") && "POST".equalsIgnoreCase(method)) {
            String phoneSegment = tail.substring(0, tail.length() - "/animals".length());
            String ownerPhone = decodePathSegment(phoneSegment);
            handleAddAnimal(exchange, ownerPhone);
            return;
        }
        if (!tail.contains("/") && "GET".equalsIgnoreCase(method)) {
            String ownerPhone = decodePathSegment(tail);
            Owner owner = service.findOwner(ownerPhone);
            if (owner == null) {
                sendJson(exchange, 404, response(false, "Ingen ägare med detta telefonnummer hittades: " + ownerPhone, null));
                return;
            }
            sendJson(exchange, 200, response(true, null, ownerData(owner)));
            return;
        }

        sendJson(exchange, 404, response(false, "Endpoint hittades inte.", null));
    }

    private void handleAddAnimal(HttpExchange exchange, String ownerPhone) throws IOException {
        Owner owner = service.findOwner(ownerPhone);
        if (owner == null) {
            sendJson(exchange, 404, response(false, "Ingen ägare med detta telefonnummer hittades: " + ownerPhone, null));
            return;
        }

        Map<String, Object> request = readJsonObject(exchange);
        String name = requireString(request, "name");
        String food = requireString(request, "food");
        String medication = requireString(request, "medication");
        String type = requireString(request, "type");
        boolean checkedIn = optionalBoolean(request, "checkedIn", false);

        DaycareResult<Animal> result = service.addAnimal(owner, name, food, medication, type, checkedIn);
        int status = result.isSuccess() ? 201 : statusForResult(result);
        Object data = result.isSuccess() ? animalData(result.getValue()) : null;
        sendJson(exchange, status, response(result.isSuccess(), result.getMessage(), data));
    }

    private void handleCheckIn(HttpExchange exchange) throws IOException {
        Map<String, Object> request = readJsonObject(exchange);
        String phone = requireString(request, "phone");
        String animalName = requireString(request, "animalName");

        Owner owner = service.findOwner(phone);
        if (owner == null) {
            sendJson(exchange, 404, response(false, "Ingen ägare med detta telefonnummer hittades: " + phone, null));
            return;
        }

        DaycareResult<Animal> result = service.checkInAnimal(owner, animalName);
        int status = result.isSuccess() ? 200 : statusForResult(result);
        Object data = result.isSuccess() ? animalData(result.getValue()) : null;
        sendJson(exchange, status, response(result.isSuccess(), result.getMessage(), data));
    }

    private void handleCheckOut(HttpExchange exchange) throws IOException {
        Map<String, Object> request = readJsonObject(exchange);
        String phone = requireString(request, "phone");
        String animalName = requireString(request, "animalName");

        Owner owner = service.findOwner(phone);
        if (owner == null) {
            sendJson(exchange, 404, response(false, "Ingen ägare med detta telefonnummer hittades: " + phone, null));
            return;
        }

        DaycareResult<Animal> result = service.checkOutAnimal(owner, animalName);
        int status = result.isSuccess() ? 200 : statusForResult(result);
        Object data = result.isSuccess() ? animalData(result.getValue()) : null;
        sendJson(exchange, status, response(result.isSuccess(), result.getMessage(), data));
    }

    private void handleTransfer(HttpExchange exchange) throws IOException {
        Map<String, Object> request = readJsonObject(exchange);
        String oldOwnerPhone = requireString(request, "oldOwnerPhone");
        String animalName = requireString(request, "animalName");
        String newOwnerPhone = requireString(request, "newOwnerPhone");

        DaycareResult<Animal> result = service.transferAnimal(oldOwnerPhone, animalName, newOwnerPhone);
        int status = result.isSuccess() ? 200 : statusForResult(result);
        Object data = result.isSuccess() ? animalData(result.getValue()) : null;
        sendJson(exchange, status, response(result.isSuccess(), result.getMessage(), data));
    }

    private Map<String, Object> readJsonObject(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        if (body.isBlank()) {
            throw new IllegalArgumentException("Request body saknas.");
        }
        return SimpleJson.parseObject(body);
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Map<String, Object> body) throws IOException {
        byte[] responseBytes = SimpleJson.stringify(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        } finally {
            exchange.close();
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private Map<String, Object> response(boolean success, String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> healthData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "daycare-api");
        data.put("version", 1);
        return data;
    }

    private List<Object> ownersData(List<Owner> owners) {
        List<Object> ownerList = new ArrayList<>();
        for (Owner owner : owners) {
            ownerList.add(ownerData(owner));
        }
        return ownerList;
    }

    private Map<String, Object> ownerData(Owner owner) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", owner.getName());
        data.put("phone", owner.getPhone());

        List<Object> animals = new ArrayList<>();
        for (Animal animal : owner.getAnimals()) {
            animals.add(animalData(animal));
        }
        data.put("animals", animals);
        return data;
    }

    private Map<String, Object> animalData(Animal animal) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", animal.getClass().getSimpleName());
        data.put("name", animal.getName());
        data.put("food", animal.getFood());
        data.put("medication", animal.getMedication());
        data.put("checkedIn", animal.isCheckedIn());
        return data;
    }

    private List<Object> checkedInAnimalsData() {
        List<Object> checkedIn = new ArrayList<>();
        for (Owner owner : service.getAllOwners()) {
            for (Animal animal : owner.getAnimals()) {
                if (animal.isCheckedIn()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("ownerName", owner.getName());
                    item.put("ownerPhone", owner.getPhone());
                    item.put("animal", animalData(animal));
                    checkedIn.add(item);
                }
            }
        }
        return checkedIn;
    }

    private String requireString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof String && !((String) value).isBlank()) {
            return (String) value;
        }
        throw new IllegalArgumentException("Fältet \"" + key + "\" saknas eller är tomt.");
    }

    private boolean optionalBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new IllegalArgumentException("Fältet \"" + key + "\" måste vara true eller false.");
    }

    private String decodePathSegment(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private int statusForResult(DaycareResult<?> result) {
        String message = result.getMessage();
        if (message != null && (
                message.startsWith("Ingen ägare") ||
                message.startsWith("Djuret hittades inte.")
        )) {
            return 404;
        }
        return 400;
    }
}
