import type { ApiEnvelope, Animal, CheckedInAnimal, HealthData, Owner } from "./types";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api").replace(/\/$/, "");

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  const payload = (await response.json()) as ApiEnvelope<T>;

  if (!response.ok || !payload.success) {
    throw new Error(payload.message ?? `Request failed with status ${response.status}`);
  }

  return payload.data;
}

export const api = {
  getHealth: () => request<HealthData>("/health"),
  getOwners: () => request<Owner[]>("/owners"),
  getCheckedIn: () => request<CheckedInAnimal[]>("/checked-in"),
  registerOwner: (owner: Pick<Owner, "name" | "phone">) =>
    request<Owner>("/owners", {
      method: "POST",
      body: JSON.stringify(owner)
    }),
  addAnimal: (
    ownerPhone: string,
    animal: Pick<Animal, "name" | "food" | "medication" | "type" | "checkedIn">
  ) =>
    request<Animal>(`/owners/${encodeURIComponent(ownerPhone)}/animals`, {
      method: "POST",
      body: JSON.stringify(animal)
    }),
  checkIn: (phone: string, animalName: string) =>
    request<Animal>("/check-in", {
      method: "POST",
      body: JSON.stringify({ phone, animalName })
    }),
  checkOut: (phone: string, animalName: string) =>
    request<Animal>("/check-out", {
      method: "POST",
      body: JSON.stringify({ phone, animalName })
    }),
  transfer: (oldOwnerPhone: string, animalName: string, newOwnerPhone: string) =>
    request<Animal>("/transfer", {
      method: "POST",
      body: JSON.stringify({ oldOwnerPhone, animalName, newOwnerPhone })
    })
};
