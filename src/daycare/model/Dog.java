package daycare.model;

public class Dog extends Animal {
    public Dog(String name, String food, String medication) {
        super(name, food, medication);
    }

    @Override
    public void makeSound() {
        System.out.println("\033[1m\033[3m –– Voff-voff! –– \033[0m");
    }
}
