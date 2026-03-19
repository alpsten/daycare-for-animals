package daycare.model;

import java.util.ArrayList;
import java.util.List;

public class Owner {
    private final String name;
    private final String phone;
    private final List<Animal> animals = new ArrayList<>();

    public Owner(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public Owner() {
        this("", "");
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public boolean addAnimal(Animal animal) {
        if (animal == null || hasAnimalNamed(animal.getName())) {
            return false;
        }
        animals.add(animal);
        return true;
    }

    public Animal getAnimalByName(String name) {
        for (Animal animal : animals) {
            if (animal.getName().equalsIgnoreCase(name)) {
                return animal;
            }
        }
        return null;
    }

    public List<Animal> getAnimals() {
        return animals;
    }

    public boolean hasAnimalNamed(String name) {
        for (Animal animal : animals) {
            if (animal.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeAnimal(Animal animal) {
        return animals.remove(animal);
    }

}
