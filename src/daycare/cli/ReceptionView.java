package daycare.cli;

import java.util.Scanner;

public class ReceptionView {
    private final Scanner scanner = new Scanner(System.in);

    public void displayMenu() {
        displayMessage("\nVälkommen till Djurdagiset!\n");
        displayMessage("1. Lämna Djur");
        displayMessage("2. Hämta Djur");
        displayMessage("3. Visa Ägare & Djur");
        displayMessage("4. Registrera Ny Ägare");
        displayMessage("5. Byt Ägare");
        displayMessage("6. Information Om Djur");
        displayMessage("7. Avsluta");
        System.out.print("\nVälj Ett Alternativ: ");
    }

    public String getInput() {
        return scanner.nextLine().trim();
    }

    public String prompt(String message) {
        displayMessage(message);
        return getInput();
    }

    public void displayMessage(String message) {
        System.out.println(message);
    }

    public void beep() {
        System.out.print("\u0007");
        System.out.flush();
    }

}
