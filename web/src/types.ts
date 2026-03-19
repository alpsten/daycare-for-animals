export type Animal = {
  type: string;
  name: string;
  food: string;
  medication: string;
  checkedIn: boolean;
};

export type Owner = {
  name: string;
  phone: string;
  animals: Animal[];
};

export type CheckedInAnimal = {
  ownerName: string;
  ownerPhone: string;
  animal: Animal;
};

export type ApiEnvelope<T> = {
  success: boolean;
  message: string | null;
  data: T;
};

export type HealthData = {
  service: string;
  version: number;
};
