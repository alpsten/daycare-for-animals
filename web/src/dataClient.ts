import { api } from "./api";
import type { Animal, CheckedInAnimal, HealthData, Owner } from "./types";

export type DataMode = "api" | "browser";

export type DataClient = {
  mode: DataMode;
  getHealth: () => Promise<HealthData>;
  getOwners: () => Promise<Owner[]>;
  getCheckedIn: () => Promise<CheckedInAnimal[]>;
  registerOwner: (owner: Pick<Owner, "name" | "phone">) => Promise<Owner>;
  addAnimal: (
    ownerPhone: string,
    animal: Pick<Animal, "name" | "food" | "medication" | "type" | "checkedIn">
  ) => Promise<Animal>;
  checkIn: (phone: string, animalName: string) => Promise<Animal>;
  checkOut: (phone: string, animalName: string) => Promise<Animal>;
  transfer: (oldOwnerPhone: string, animalName: string, newOwnerPhone: string) => Promise<Animal>;
};

const DEMO_STORAGE_KEY = "daycare-for-animals.demo.v2";
const LEGACY_DEMO_STORAGE_KEYS = ["daycare-for-animals.demo.v1"];
const DATA_MODE = (import.meta.env.VITE_DATA_MODE ?? "auto").toLowerCase();

const legacyPlaceholderOwnerNames = ["Carolina", "Danny", "Robin", "Tilde"];
const replacementOwnerNames = ["Jane Doe", "John Doe", "Alex Smith", "Sam Taylor"];

const demoSeedOwners: Owner[] = [
  {
    name: "Jane Doe",
    phone: "07012341234",
    animals: [
      { type: "Dog", name: "Rex", food: "Dry food", medication: "None", checkedIn: true },
      { type: "Bird", name: "Daisy", food: "Seeds", medication: "None", checkedIn: false }
    ]
  },
  {
    name: "John Doe",
    phone: "07212341234",
    animals: [{ type: "Cat", name: "Dallas", food: "Wet food", medication: "None", checkedIn: true }]
  },
  {
    name: "Alex Smith",
    phone: "07312341234",
    animals: [{ type: "Cat", name: "Texas", food: "Dry food", medication: "None", checkedIn: false }]
  },
  {
    name: "Sam Taylor",
    phone: "07612341234",
    animals: [{ type: "Dog", name: "Buddy", food: "Dry food", medication: "Joint care", checkedIn: false }]
  }
];

let resolvedClientPromise: Promise<DataClient> | null = null;

const browserClient: DataClient = {
  mode: "browser",
  getHealth: async () => ({ service: "browser-storage", version: 1 }),
  getOwners: async () => loadOwners(),
  getCheckedIn: async () => getCheckedInAnimals(loadOwners()),
  registerOwner: async (ownerInput) => {
    const owners = loadOwners();
    if (findOwnerByPhone(owners, ownerInput.phone)) {
      throw new Error("An owner with this phone number already exists.");
    }

    const owner: Owner = {
      name: ownerInput.name.trim(),
      phone: ownerInput.phone.trim(),
      animals: []
    };
    owners.push(owner);
    saveOwners(owners);
    return owner;
  },
  addAnimal: async (ownerPhone, animalInput) => {
    const owners = loadOwners();
    const owner = findOwnerByPhone(owners, ownerPhone);
    if (!owner) {
      throw new Error("Owner not found.");
    }
    if (findAnimalByName(owner, animalInput.name)) {
      throw new Error("An animal with this name already exists for this owner.");
    }

    const animal: Animal = {
      name: animalInput.name.trim(),
      food: animalInput.food.trim(),
      medication: animalInput.medication.trim(),
      checkedIn: animalInput.checkedIn,
      type: normalizeAnimalType(animalInput.type)
    };
    owner.animals.push(animal);
    saveOwners(owners);
    return animal;
  },
  checkIn: async (phone, animalName) => {
    const owners = loadOwners();
    const owner = findOwnerByPhone(owners, phone);
    if (!owner) {
      throw new Error("Owner not found.");
    }
    const animal = findAnimalByName(owner, animalName);
    if (!animal) {
      throw new Error("Animal not found.");
    }
    if (animal.checkedIn) {
      throw new Error(`${animal.name} is already checked in.`);
    }

    animal.checkedIn = true;
    saveOwners(owners);
    return animal;
  },
  checkOut: async (phone, animalName) => {
    const owners = loadOwners();
    const owner = findOwnerByPhone(owners, phone);
    if (!owner) {
      throw new Error("Owner not found.");
    }
    const animal = findAnimalByName(owner, animalName);
    if (!animal) {
      throw new Error("Animal not found.");
    }
    if (!animal.checkedIn) {
      throw new Error(`${animal.name} is not checked in.`);
    }

    animal.checkedIn = false;
    saveOwners(owners);
    return animal;
  },
  transfer: async (oldOwnerPhone, animalName, newOwnerPhone) => {
    const owners = loadOwners();
    const oldOwner = findOwnerByPhone(owners, oldOwnerPhone);
    const newOwner = findOwnerByPhone(owners, newOwnerPhone);

    if (!oldOwner) {
      throw new Error("Current owner not found.");
    }
    if (!newOwner) {
      throw new Error("New owner not found.");
    }
    if (normalizePhone(oldOwnerPhone) === normalizePhone(newOwnerPhone)) {
      throw new Error("The animal already belongs to this owner.");
    }

    const animal = findAnimalByName(oldOwner, animalName);
    if (!animal) {
      throw new Error("Animal not found.");
    }
    if (findAnimalByName(newOwner, animalName)) {
      throw new Error("The new owner already has an animal with this name.");
    }

    oldOwner.animals = oldOwner.animals.filter((entry) => !sameAnimalName(entry.name, animalName));
    newOwner.animals.push(animal);
    saveOwners(owners);
    return animal;
  }
};

export async function getDataClient(): Promise<DataClient> {
  if (resolvedClientPromise) {
    return resolvedClientPromise;
  }

  resolvedClientPromise = resolveClient();
  return resolvedClientPromise;
}

async function resolveClient(): Promise<DataClient> {
  if (DATA_MODE === "browser") {
    return browserClient;
  }

  if (DATA_MODE === "api") {
    return { ...api, mode: "api" };
  }

  try {
    await api.getHealth();
    return { ...api, mode: "api" };
  } catch {
    return browserClient;
  }
}

function loadOwners(): Owner[] {
  if (typeof window === "undefined") {
    return structuredClone(demoSeedOwners);
  }

  const stored = window.localStorage.getItem(DEMO_STORAGE_KEY);
  if (!stored) {
    const migratedLegacyOwners = loadLegacyOwners();
    if (migratedLegacyOwners) {
      saveOwners(migratedLegacyOwners);
      return structuredClone(migratedLegacyOwners);
    }

    saveOwners(demoSeedOwners);
    return structuredClone(demoSeedOwners);
  }

  try {
    const owners = migrateOwners(JSON.parse(stored) as Owner[]);
    saveOwners(owners);
    return structuredClone(owners);
  } catch {
    saveOwners(demoSeedOwners);
    return structuredClone(demoSeedOwners);
  }
}

function saveOwners(owners: Owner[]) {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(owners));
  }
}

function getCheckedInAnimals(owners: Owner[]): CheckedInAnimal[] {
  return owners.flatMap((owner) =>
    owner.animals
      .filter((animal) => animal.checkedIn)
      .map((animal) => ({
        ownerName: owner.name,
        ownerPhone: owner.phone,
        animal
      }))
  );
}

function findOwnerByPhone(owners: Owner[], phone: string) {
  return owners.find((owner) => normalizePhone(owner.phone) === normalizePhone(phone));
}

function findAnimalByName(owner: Owner, animalName: string) {
  return owner.animals.find((animal) => sameAnimalName(animal.name, animalName));
}

function sameAnimalName(left: string, right: string) {
  return left.trim().toLowerCase() === right.trim().toLowerCase();
}

function normalizePhone(phone: string) {
  return phone.trim();
}

function normalizeAnimalType(type: string) {
  switch (type.trim().toLowerCase()) {
    case "hund":
    case "dog":
      return "Dog";
    case "katt":
    case "cat":
      return "Cat";
    case "fågel":
    case "fagel":
    case "bird":
      return "Bird";
    default:
      throw new Error("Please choose a valid animal type.");
  }
}

function loadLegacyOwners() {
  for (const key of LEGACY_DEMO_STORAGE_KEYS) {
    const stored = window.localStorage.getItem(key);
    if (!stored) {
      continue;
    }

    try {
      return migrateOwners(JSON.parse(stored) as Owner[]);
    } catch {
      window.localStorage.removeItem(key);
    }
  }

  return null;
}

function migrateOwners(owners: Owner[]) {
  return owners.map((owner) => {
    const replacementIndex = legacyPlaceholderOwnerNames.findIndex(
      (legacyName) => legacyName.toLowerCase() === owner.name.trim().toLowerCase()
    );

    if (replacementIndex === -1) {
      return owner;
    }

    return {
      ...owner,
      name: replacementOwnerNames[replacementIndex]
    };
  });
}
