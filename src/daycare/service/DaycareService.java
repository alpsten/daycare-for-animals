package daycare.service;

import daycare.model.Animal;
import daycare.model.Owner;
import daycare.storage.OwnerStorage;

import java.util.List;

public class DaycareService {
    private final OwnerStorage storage;
    private final OwnerManager ownerManager;
    private final AnimalManager animalManager;

    public DaycareService(OwnerStorage storage) {
        this.storage = storage;
        this.ownerManager = new OwnerManager();
        this.animalManager = new AnimalManager();
        ownerManager.setOwners(storage.loadOwners());
    }

    public Owner findOwner(String phone) {
        return ownerManager.findOwner(phone);
    }

    public List<Owner> getAllOwners() {
        return ownerManager.getAllOwners();
    }

    public DaycareResult<Owner> registerOwner(String name, String phone) {
        Owner owner = new Owner(name, phone);
        if (!ownerManager.addOwner(owner)) {
            return DaycareResult.failure("En ägare finns redan med detta telefonnummer.");
        }

        saveOwners();
        return DaycareResult.success("Ny ägare registrerad.", owner);
    }

    public DaycareResult<Animal> addAnimal(Owner owner, String name, String food, String medication, String type, boolean checkedIn) {
        DaycareResult<Animal> result = animalManager.addAnimal(owner, name, food, medication, type, checkedIn);
        if (result.isSuccess()) {
            saveOwners();
        }
        return result;
    }

    public DaycareResult<Animal> checkInAnimal(Owner owner, String animalName) {
        DaycareResult<Animal> result = animalManager.checkInAnimal(owner, animalName);
        if (result.isSuccess()) {
            saveOwners();
        }
        return result;
    }

    public DaycareResult<Animal> checkOutAnimal(Owner owner, String animalName) {
        DaycareResult<Animal> result = animalManager.checkOutAnimal(owner, animalName);
        if (result.isSuccess()) {
            saveOwners();
        }
        return result;
    }

    public DaycareResult<Animal> transferAnimal(String oldOwnerPhone, String animalName, String newOwnerPhone) {
        DaycareResult<Animal> result = ownerManager.transferAnimal(oldOwnerPhone, animalName, newOwnerPhone);
        if (result.isSuccess()) {
            saveOwners();
        }
        return result;
    }

    public String getAllCheckedInAnimals() {
        StringBuilder message = new StringBuilder("\nFöljande djur är fortfarande incheckade:\n");
        boolean hasCheckedInAnimals = false;

        for (Owner owner : ownerManager.getAllOwners()) {
            for (Animal animal : owner.getAnimals()) {
                if (animal.isCheckedIn()) {
                    hasCheckedInAnimals = true;
                    message.append("- ").append(animal.getName())
                            .append(" (Ägare: ").append(owner.getName()).append(")\n");
                }
            }
        }

        return hasCheckedInAnimals ? message.toString() : null;
    }

    public void saveOwners() {
        storage.saveOwners(ownerManager.getAllOwners());
    }
}
