package daycare.storage;

import daycare.model.Animal;
import daycare.model.Bird;
import daycare.model.Cat;
import daycare.model.Dog;
import daycare.model.Owner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileHandler implements OwnerStorage {
    private static final Path LEGACY_FILE = Paths.get("src", "owners.txt");
    private static final Path DEFAULT_FILE = Paths.get("data", "owners.txt");
    private static final String DATA_FILE_PROPERTY = "daycare.data.file";

    private final Path dataFile;
    private final boolean usingDefaultPath;

    public FileHandler() {
        this(resolveStoragePath());
    }

    public FileHandler(Path dataFile) {
        this.dataFile = dataFile;
        String configuredFile = System.getProperty(DATA_FILE_PROPERTY);
        this.usingDefaultPath = configuredFile == null || configuredFile.isBlank();
    }

    @Override
    public void saveOwners(List<Owner> owners) {
        try {
            ensureParentDirectoryExists();
        } catch (IOException e) {
            System.out.println("Fel vid sparning av ägare: " + e.getMessage());
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(dataFile)) {
            for (Owner owner : owners) {
                writeOwner(writer, owner);
            }
        } catch (IOException e) {
            System.out.println("Fel vid sparning av ägare: " + e.getMessage());
        }
    }

    private void writeOwner(BufferedWriter writer, Owner owner) throws IOException {
        writer.write(owner.getName() + ";" + owner.getPhone());
        writer.newLine();

        for (Animal animal : owner.getAnimals()) {
            writer.write(
                    animal.getClass().getSimpleName() + ";" +
                            animal.getName() + ";" +
                            animal.getFood() + ";" +
                            animal.getMedication() + ";" +
                            animal.isCheckedIn());
            writer.newLine();
        }
        writer.newLine(); // Tom rad mellan ägare i (owners.txt).
    }

    @Override
    public void appendOwner(Owner owner) {
        try {
            ensureParentDirectoryExists();
        } catch (IOException e) {
            System.out.println("Fel vid sparning av ägare: " + e.getMessage());
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                dataFile,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
        )) {
            writeOwner(writer, owner);
        } catch (IOException e) {
            System.out.println("Fel vid sparning av ägare: " + e.getMessage());
        }
    }

    public void appendOwnerToFile(Owner owner) {
        appendOwner(owner);
    }

    public List<Owner> loadOwners() {
        List<Owner> owners = new ArrayList<>();
        Path loadPath = getLoadPath();

        try (BufferedReader reader = Files.newBufferedReader(loadPath)) {
            String line;
            Owner currentOwner = null;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    currentOwner = null;
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length == 2) {
                    Owner existingOwner = findOwnerByPhone(owners, parts[1]);
                    if (existingOwner != null) {
                        System.out.println("Duplicerad ägare ignorerades för telefonnummer: " + parts[1]);
                        currentOwner = existingOwner;
                        continue;
                    }

                    currentOwner = new Owner(parts[0], parts[1]);
                    owners.add(currentOwner);
                } else if (parts.length == 5 && currentOwner != null) {
                    String type = parts[0];
                    String name = parts[1];
                    String food = parts[2];
                    String medication = parts[3];
                    boolean isCheckedIn = Boolean.parseBoolean(parts[4]);

                    Animal animal;
                    switch (type.toLowerCase()) {
                        case "dog":
                            animal = new Dog(name, food, medication);
                            break;
                        case "cat":
                            animal = new Cat(name, food, medication);
                            break;
                        case "bird":
                            animal = new Bird(name, food, medication);
                            break;
                        default:
                            System.out.println("Okänd djurtyp: " + type);
                            continue;
                    }
                    animal.setCheckedIn(isCheckedIn);
                    if (!currentOwner.addAnimal(animal)) {
                        System.out.println("Duplicerat djur ignorerades för ägaren " + currentOwner.getName() + ": " + name);
                    }
                }
            }
        } catch (NoSuchFileException e) {
            System.out.println("Ingen tidigare data hittades. En ny fil skapas vid sparning.");
        } catch (IOException e) {
            System.out.println("Fel vid laddning av ägare: " + e.getMessage());
        }
        return owners;
    }

    private static Path resolveStoragePath() {
        String configuredFile = System.getProperty(DATA_FILE_PROPERTY);
        if (configuredFile != null && !configuredFile.isBlank()) {
            return Paths.get(configuredFile);
        }
        return DEFAULT_FILE;
    }

    private Path getLoadPath() {
        if (Files.exists(dataFile)) {
            return dataFile;
        }
        if (usingDefaultPath && Files.exists(LEGACY_FILE)) {
            return LEGACY_FILE;
        }
        return dataFile;
    }

    private void ensureParentDirectoryExists() throws IOException {
        Path parent = dataFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private Owner findOwnerByPhone(List<Owner> owners, String phone) {
        for (Owner owner : owners) {
            if (owner.getPhone().trim().equals(phone.trim())) {
                return owner;
            }
        }
        return null;
    }
}
