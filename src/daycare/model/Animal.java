package daycare.model;

public abstract class Animal implements IAnimal {
    private final String name;
    private final String food;
    private final String medication;
    private boolean checkedIn = false;

    public Animal(String name, String food, String medication) {
        this.name = name;
        this.food = food;
        this.medication = medication;
    }

    public String getName() {
        return name;
    }

    public String getFood() {
        return food;
    }

    public String getMedication() {
        return medication;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public abstract void makeSound();

    public String getInfo() {
        return "Namn: " + getName() + " (" + getClass().getSimpleName() + "), Mat: " + getFood() + ", Medicin: " + getMedication();
    }
}
