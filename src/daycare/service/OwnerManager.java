package daycare.service;

import daycare.model.Animal;
import daycare.model.Owner;

import java.util.ArrayList;
import java.util.List;

public class OwnerManager {
    private List<Owner> owners = new ArrayList<>();

    public OwnerManager() {
    }

    public boolean addOwner(Owner owner) {
        if (owner == null || findOwner(owner.getPhone()) != null) {
            return false;
        }
        owners.add(owner);
        return true;
    }

    public Owner findOwner(String phone) {
        String normalizedPhone = normalizePhone(phone);
        for (Owner owner : owners) {
            if (normalizePhone(owner.getPhone()).equals(normalizedPhone)) {
                return owner;
            }
        }
        return null;
    }

    public DaycareResult<Animal> transferAnimal(String oldOwnerPhone, String animalName, String newOwnerPhone) {
        Owner oldOwner = findOwner(oldOwnerPhone);
        if (oldOwner == null) {
            return DaycareResult.failure("Ingen ägare med detta telefonnummer hittades: " + oldOwnerPhone);
        }

        Animal petGettingNewOwner = oldOwner.getAnimalByName(animalName);
        if (petGettingNewOwner == null) {
            return DaycareResult.failure("Djuret finns inte. Försök igen.");
        }

        if (normalizePhone(newOwnerPhone).equals(normalizePhone(oldOwnerPhone))) {
            return DaycareResult.failure("Djuret har redan denna ägare.");
        }

        Owner newOwner = findOwner(newOwnerPhone);
        if (newOwner == null) {
            return DaycareResult.failure("Ingen ägare hittad med detta telefonnummer: " + newOwnerPhone);
        }

        if (!transferOwnershipForAnimal(oldOwner, newOwner, petGettingNewOwner)) {
            return DaycareResult.failure("Djuret kunde inte byta ägare. Kontrollera att namnet inte redan finns hos den nya ägaren.");
        }

        return DaycareResult.success("Djuret " + petGettingNewOwner.getName() + " har bytt ägare från "
                + oldOwner.getName() + " till " + newOwner.getName() + ".", petGettingNewOwner);
    }

    public boolean transferOwnershipForAnimal(Owner oldOwner, Owner newOwner, Animal petGettingNewOwner) {
        if (oldOwner == null || newOwner == null || petGettingNewOwner == null) {
            return false;
        }
        if (!oldOwner.removeAnimal(petGettingNewOwner)) {
            return false;
        }
        if (newOwner.addAnimal(petGettingNewOwner)) {
            return true;
        }

        oldOwner.addAnimal(petGettingNewOwner);
        return false;
    }

    // Returnerar en kopia av listan
    public List<Owner> getAllOwners() {
        return new ArrayList<>(owners);
    }

    public void setOwners(List<Owner> owners) {
        this.owners = owners;
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

}
