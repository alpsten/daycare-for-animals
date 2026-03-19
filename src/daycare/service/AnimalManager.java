package daycare.service;

import daycare.model.Animal;
import daycare.model.Bird;
import daycare.model.Cat;
import daycare.model.Dog;
import daycare.model.Owner;

public class AnimalManager {
    public AnimalManager() {
    }

    public DaycareResult<Animal> checkInAnimal(Owner owner, String name) {
        Animal animal = owner.getAnimalByName(name);
        if (animal == null) {
            return DaycareResult.failure("Djuret hittades inte.");
        }

        if (animal.isCheckedIn()) {
            return DaycareResult.failure("Djuret " + name + " är redan incheckat.");
        }

        animal.setCheckedIn(true);
        return DaycareResult.success(name + " har checkats in.", animal);
    }

    public DaycareResult<Animal> checkOutAnimal(Owner owner, String name) {
        Animal animal = owner.getAnimalByName(name);
        if (animal == null) {
            return DaycareResult.failure("Djuret hittades inte.");
        }

        if (!animal.isCheckedIn()) {
            return DaycareResult.failure("Djuret " + name + " är inte incheckat.");
        }

        animal.setCheckedIn(false);
        return DaycareResult.success(name + " har hämtats.", animal);
    }

    public DaycareResult<Animal> addAnimal(Owner owner, String name, String food, String medication, String type, boolean checkedIn) {
        if (owner.hasAnimalNamed(name)) {
            return DaycareResult.failure("Djuret " + name + " finns redan registrerat hos " + owner.getName() + ".");
        }

        Animal animal;
        switch (type.toLowerCase()) {
            case "hund":
            case "dog":
                animal = new Dog(name, food, medication);
                break;
            case "katt":
            case "cat":
                animal = new Cat(name, food, medication);
                break;
            case "fågel":
            case "fagel":
            case "bird":
                animal = new Bird(name, food, medication);
                break;
            default:
                return DaycareResult.failure("Ogiltig djurtyp. Djuret registrerades inte.");
        }

        animal.setCheckedIn(checkedIn);
        if (!owner.addAnimal(animal)) {
            return DaycareResult.failure("Djuret " + name + " kunde inte registreras.");
        }

        return DaycareResult.success("Djuret " + name + " har lagts till hos ägaren " + owner.getName() + ".", animal);
    }

    public String formatAnimal(Owner owner, Animal animal) {
        String animalType = animal.getClass().getSimpleName();
        String status = animal.isCheckedIn() ? "Incheckad" : "Ej incheckad";
        return "Ägare: " + owner.getName() +
                ", Tele: " + owner.getPhone() +
                ", " + animalType +
                ", Namn: " + animal.getName() +
                ", Mat: " + animal.getFood() +
                ", Medicin: " + animal.getMedication() +
                ", Status: " + status;
    }
}
