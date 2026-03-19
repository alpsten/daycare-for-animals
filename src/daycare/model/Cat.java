package daycare.model;

public class Cat extends Animal {
    public Cat(String name, String food, String medication) {
        super(name, food, medication);
    }

    @Override
    public void makeSound() {
        System.out.println("\033[1m\033[3m –– Mjau-mjau! –– \033[0m");
    }
}
