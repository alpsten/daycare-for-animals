package daycare.storage;

import daycare.model.Animal;
import daycare.model.Bird;
import daycare.model.Cat;
import daycare.model.Dog;
import daycare.model.Owner;
import daycare.util.SimpleJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonOwnerStorage implements OwnerStorage {
    private static final Path DEFAULT_FILE = Paths.get("data", "owners.json");
    private static final String DATA_FILE_PROPERTY = "daycare.data.file";

    private final Path dataFile;
    private final OwnerStorage legacyStorage;

    public JsonOwnerStorage() {
        this(resolveStoragePath(), new FileHandler());
    }

    public JsonOwnerStorage(Path dataFile, OwnerStorage legacyStorage) {
        this.dataFile = dataFile;
        this.legacyStorage = legacyStorage;
    }

    @Override
    public List<Owner> loadOwners() {
        try {
            String json = Files.readString(dataFile);
            return parseOwners(json);
        } catch (NoSuchFileException e) {
            return legacyStorage.loadOwners();
        } catch (IOException e) {
            System.out.println("Fel vid laddning av JSON-data: " + e.getMessage());
            return new ArrayList<>();
        } catch (IllegalArgumentException e) {
            System.out.println("Felaktig JSON-data: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void saveOwners(List<Owner> owners) {
        try {
            ensureParentDirectoryExists();
            Files.writeString(dataFile, SimpleJson.stringify(toOwnerData(owners)));
        } catch (IOException e) {
            System.out.println("Fel vid sparning av JSON-data: " + e.getMessage());
        }
    }

    @Override
    public void appendOwner(Owner owner) {
        List<Owner> owners = loadOwners();
        owners.add(owner);
        saveOwners(owners);
    }

    private static Path resolveStoragePath() {
        String configuredFile = System.getProperty(DATA_FILE_PROPERTY);
        if (configuredFile != null && !configuredFile.isBlank()) {
            return Paths.get(configuredFile);
        }
        return DEFAULT_FILE;
    }

    private void ensureParentDirectoryExists() throws IOException {
        Path parent = dataFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private List<Owner> parseOwners(String json) {
        List<Object> root = SimpleJson.parseArray(json);
        List<Owner> owners = new ArrayList<>();

        for (Object item : root) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("Rotobjektet måste innehålla ägarobjekt.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> ownerData = (Map<String, Object>) item;
            Owner owner = new Owner(getRequiredString(ownerData, "name"), getRequiredString(ownerData, "phone"));

            Object animalsValue = ownerData.get("animals");
            if (!(animalsValue instanceof List)) {
                throw new IllegalArgumentException("Ägarens djurlista saknas eller har fel format.");
            }

            @SuppressWarnings("unchecked")
            List<Object> animals = (List<Object>) animalsValue;
            for (Object animalItem : animals) {
                if (!(animalItem instanceof Map)) {
                    throw new IllegalArgumentException("Djurobjektet har fel format.");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> animalData = (Map<String, Object>) animalItem;
                Animal animal = createAnimal(
                        getRequiredString(animalData, "type"),
                        getRequiredString(animalData, "name"),
                        getRequiredString(animalData, "food"),
                        getRequiredString(animalData, "medication")
                );
                animal.setCheckedIn(getRequiredBoolean(animalData, "checkedIn"));
                if (!owner.addAnimal(animal)) {
                    System.out.println("Duplicerat djur ignorerades för ägaren " + owner.getName() + ": " + animal.getName());
                }
            }

            owners.add(owner);
        }

        return owners;
    }

    private List<Object> toOwnerData(List<Owner> owners) {
        List<Object> ownerData = new ArrayList<>();
        for (Owner owner : owners) {
            Map<String, Object> ownerMap = new java.util.LinkedHashMap<>();
            ownerMap.put("name", owner.getName());
            ownerMap.put("phone", owner.getPhone());

            List<Object> animals = new ArrayList<>();
            for (Animal animal : owner.getAnimals()) {
                Map<String, Object> animalMap = new java.util.LinkedHashMap<>();
                animalMap.put("type", animal.getClass().getSimpleName());
                animalMap.put("name", animal.getName());
                animalMap.put("food", animal.getFood());
                animalMap.put("medication", animal.getMedication());
                animalMap.put("checkedIn", animal.isCheckedIn());
                animals.add(animalMap);
            }

            ownerMap.put("animals", animals);
            ownerData.add(ownerMap);
        }
        return ownerData;
    }

    private Animal createAnimal(String type, String name, String food, String medication) {
        switch (type.toLowerCase()) {
            case "dog":
                return new Dog(name, food, medication);
            case "cat":
                return new Cat(name, food, medication);
            case "bird":
                return new Bird(name, food, medication);
            default:
                throw new IllegalArgumentException("Okänd djurtyp i JSON: " + type);
        }
    }

    private String getRequiredString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalArgumentException("Fältet \"" + key + "\" saknas eller är inte en sträng.");
    }

    private boolean getRequiredBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new IllegalArgumentException("Fältet \"" + key + "\" saknas eller är inte en boolean.");
    }
}
