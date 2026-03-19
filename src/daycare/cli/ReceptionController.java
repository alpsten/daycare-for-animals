package daycare.cli;

import daycare.model.Animal;
import daycare.model.Owner;
import daycare.service.AnimalManager;
import daycare.service.DaycareResult;
import daycare.service.DaycareService;
import daycare.storage.JsonOwnerStorage;

public class ReceptionController {
    private final ReceptionView view;
    private final DaycareService service;
    private final AnimalManager animalManager;
    private boolean returnToMenu = false;

    public ReceptionController() {
        view = new ReceptionView();
        service = new DaycareService(new JsonOwnerStorage());
        animalManager = new AnimalManager();
    }

    public void start() {
        boolean running = true;
        while (running) {
            returnToMenu = false;
            view.displayMenu();
            String choice = view.getInput().toLowerCase();

            switch (choice) {
                case "1":
                    checkIn();
                    break;
                case "2":
                    checkOut();
                    break;
                case "3":
                    listAnimals();
                    break;
                case "4":
                    registerOwner();
                    break;
                case "5":
                    changeOwner();
                    break;
                case "6":
                    getInfoOfAnimal();
                    break;
                case "7":
                    if (exitProgram()) {
                        running = false;
                    }
                    break;
                default:
                    view.displayMessage("Ogiltigt val. Försök igen.");
            }
        }
    }

    public String getAllCheckedInAnimals() {
        return service.getAllCheckedInAnimals();
    }

    private boolean exitProgram() {
        String checkedInAnimals = getAllCheckedInAnimals();

        if (checkedInAnimals != null) {
            view.displayMessage(checkedInAnimals);
            view.displayMessage("Det går inte att stänga programmet. Alla djur måste vara utcheckade!");
            return false;
        }

        service.saveOwners();
        view.displayMessage("\nAvslutar programmet. Tack för att du använde Djurdagiset!");
        return true;
    }

    public void checkIn() {
        view.displayMessage("\nAnge ägarens telefonnummer (eller skriv MENY för att återgå): ");
        String phone = getInput();
        if (returnToMenu) return;

        Owner owner = service.findOwner(phone);
        if (owner == null) {
            view.displayMessage("Ägare hittades inte. Vill du registrera en ny ägare? (Ja/Nej)");
            String response = getInput();
            if (returnToMenu) return;
            if (response.equalsIgnoreCase("JA")) {
                registerOwner();
            }
        } else {
            String animalName = view.prompt("Ange djurets namn: ");
            Animal animal = owner.getAnimalByName(animalName);
            if (animal == null) {
                String response = view.prompt("Djuret hittades inte. Vill du registrera det? (Ja/Nej)");
                if (response.equalsIgnoreCase("JA")) {
                    DaycareResult<Animal> addResult = registerAnimal(owner, animalName, true);
                    if (addResult != null && addResult.isSuccess()) {
                        view.displayMessage(animalName + " har registrerats och checkats in.");
                        view.beep();
                    }
                }
                return;
            }

            DaycareResult<Animal> result = service.checkInAnimal(owner, animalName);
            view.displayMessage(result.getMessage());
            if (result.isSuccess()) {
                view.beep();
            }
        }
    }

    public void checkOut() {
        view.displayMessage("\nAnge ägarens telefonnummer (eller skriv MENY för att återgå): ");
        String phone = getInput();
        if (returnToMenu) return;

        Owner owner = service.findOwner(phone);
        if (owner == null) {
            view.displayMessage("Ägare hittades inte.");
        } else {
            String animalName = view.prompt("Ange djurets namn: ");
            DaycareResult<Animal> result = service.checkOutAnimal(owner, animalName);
            view.displayMessage(result.getMessage());
            if (result.isSuccess()) {
                result.getValue().makeSound();
            }
        }
    }

    public void getInfoOfAnimal() {
        view.displayMessage("\nAnge ägarens telefonnummer (eller skriv MENY för att återgå): ");
        String phone = getInput();
        if (returnToMenu) return;

        Owner owner = service.findOwner(phone);
        if (owner == null) {
            view.displayMessage("Ägare hittades inte.");
        } else if (owner.getAnimals().isEmpty()) {
            view.displayMessage(owner.getName() + " har inga registrerade djur.");
        } else {
            for (Animal animal : owner.getAnimals()) {
                view.displayMessage(animal.getInfo());
            }
        }
    }

    public void listAnimals() {
        view.displayMessage("\nÄgare & Djur"); // Visar alla ägare och deras djur
        if (service.getAllOwners().isEmpty()) {
            view.displayMessage("Inga ägare eller djur är registrerade.");
            return;
        }

        for (Owner owner : service.getAllOwners()) {
            for (Animal animal : owner.getAnimals()) {
                view.displayMessage(animalManager.formatAnimal(owner, animal));
            }
        }
    }

    public void registerOwner() {
        view.displayMessage("\nAnge ägarens telefonnummer (eller skriv MENY för att återgå): ");
        String phone = getInput();
        if (returnToMenu) return;

        Owner existingOwner = service.findOwner(phone);
        if (existingOwner != null) {
            view.displayMessage("En ägare finns redan med detta telefonnummer.");
            view.displayMessage("Vill du lägga till ett djur till ägaren istället? (Ja/Nej): ");
            String response = getInput();
            if (returnToMenu) return;

            if (response.equalsIgnoreCase("JA")) {
                DaycareResult<Animal> result = registerAnimal(existingOwner, null, false);
                if (result != null && result.isSuccess()) {
                    view.displayMessage("Djuret har registrerats till ägaren " + existingOwner.getName() + ".");
                }
            } else {
                view.displayMessage("Återgår till huvudmenyn.");
            }
            return;
        }

        // Om telefonnumret inte finns, fortsätt med att registrera en ny ägare
        view.displayMessage("Ange ägarens namn (eller skriv MENY för att återgå): ");
        String name = getInput();
        if (returnToMenu) return;

        DaycareResult<Owner> ownerResult = service.registerOwner(name, phone);
        if (!ownerResult.isSuccess()) {
            view.displayMessage(ownerResult.getMessage());
            return;
        }

        view.displayMessage("Ny ägare registrerad. Vill du lägga till ett djur? (Ja/Nej): ");
        String response = getInput();
        if (returnToMenu) return;

        if (response.equalsIgnoreCase("JA")) {
            registerAnimal(ownerResult.getValue(), null, false);
        }
    }

    public void changeOwner() {
        view.displayMessage("Ange telefonnummer för gamla ägaren (eller skriv MENY för att återgå): ");
        String oldOwnerPhone = getInput();
        if (returnToMenu) return;

        Owner oldOwner = service.findOwner(oldOwnerPhone);
        if (oldOwner == null) {
            view.displayMessage("Ingen ägare med detta telefonnummer hittades: " + oldOwnerPhone);
            return;
        }
        if (oldOwner.getAnimals().isEmpty()) {
            view.displayMessage(oldOwner.getName() + " har inga husdjur som kan bytas.");
            return;
        }

        view.displayMessage("Vilket husdjur ska byta ägare?");
        String animalName = promptForAnimalSelection(oldOwner);
        if (returnToMenu) return;

        String newOwnerPhone = view.prompt("Ange telefonnumret för den nya ägaren: ");
        DaycareResult<Animal> result = service.transferAnimal(oldOwnerPhone, animalName, newOwnerPhone);
        view.displayMessage(result.getMessage());
    }

    public String getInput() {
        String input = view.getInput().trim();
        if (input.equalsIgnoreCase("MENY")) {
            returnToMenu = true;
        }
        return input;
    }

    private DaycareResult<Animal> registerAnimal(Owner owner, String knownName, boolean checkedIn) {
        String animalName = knownName != null ? knownName : view.prompt("Ange djurets namn: ");
        String food = view.prompt("Ange matvanor: ");
        String medication = view.prompt("Ange medicin: ");
        String type = view.prompt("Ange djurtyp (Hund, Katt, Fågel): ");

        DaycareResult<Animal> result = service.addAnimal(owner, animalName, food, medication, type, checkedIn);
        view.displayMessage(result.getMessage());
        return result;
    }

    private String promptForAnimalSelection(Owner owner) {
        while (true) {
            for (Animal animal : owner.getAnimals()) {
                view.displayMessage("- " + animal.getName());
            }

            String selectedPet = getInput();
            if (returnToMenu) {
                return null;
            }
            if (owner.getAnimalByName(selectedPet) != null) {
                return selectedPet;
            }
            view.displayMessage("Djuret finns inte. Försök igen.");
        }
    }

}
