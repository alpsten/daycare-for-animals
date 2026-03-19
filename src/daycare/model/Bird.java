package daycare.model;

public class Bird extends Animal {
    public Bird(String name, String food, String medication) {
        super(name, food, medication);
    }

    @Override
    public void makeSound() {
        System.out.println("\033[1m\033[3m –– Kvitter-kvitter! –– \033[0m");
    }
}
