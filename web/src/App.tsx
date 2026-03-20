import { FormEvent, useDeferredValue, useEffect, useRef, useState } from "react";
import { getDataClient } from "./dataClient";
import type { CheckedInAnimal, HealthData, Owner } from "./types";

type OwnerFormState = {
  name: string;
  phone: string;
};

type AnimalFormState = {
  ownerPhone: string;
  name: string;
  food: string;
  medication: string;
  type: string;
  checkedIn: boolean;
};

type ActionFormState = {
  phone: string;
  animalName: string;
};

type TransferFormState = {
  oldOwnerPhone: string;
  animalName: string;
  newOwnerPhone: string;
};

type ActionSectionId =
  | "register-owner"
  | "register-animal"
  | "check-in"
  | "check-out"
  | "transfer";

const emptyOwnerForm: OwnerFormState = { name: "", phone: "" };
const emptyAnimalForm: AnimalFormState = {
  ownerPhone: "",
  name: "",
  food: "",
  medication: "",
  type: "hund",
  checkedIn: false
};
const emptyActionForm: ActionFormState = { phone: "", animalName: "" };
const emptyTransferForm: TransferFormState = {
  oldOwnerPhone: "",
  animalName: "",
  newOwnerPhone: ""
};

export default function App() {
  const clientRef = useRef<Awaited<ReturnType<typeof getDataClient>> | null>(null);
  const [owners, setOwners] = useState<Owner[]>([]);
  const [checkedInAnimals, setCheckedInAnimals] = useState<CheckedInAnimal[]>([]);
  const [health, setHealth] = useState<HealthData | null>(null);
  const [dataMode, setDataMode] = useState<"api" | "browser" | "unknown">("unknown");
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [ownerForm, setOwnerForm] = useState<OwnerFormState>(emptyOwnerForm);
  const [animalForm, setAnimalForm] = useState<AnimalFormState>(emptyAnimalForm);
  const [checkInForm, setCheckInForm] = useState<ActionFormState>(emptyActionForm);
  const [checkOutForm, setCheckOutForm] = useState<ActionFormState>(emptyActionForm);
  const [transferForm, setTransferForm] = useState<TransferFormState>(emptyTransferForm);
  const [openSection, setOpenSection] = useState<ActionSectionId | null>("register-owner");
  const [openOwnerPhone, setOpenOwnerPhone] = useState<string | null>(null);

  useEffect(() => {
    void loadDashboard();
  }, []);

  async function loadDashboard() {
    try {
      setLoading(true);
      setError(null);
      if (!clientRef.current) {
        clientRef.current = await getDataClient();
        setDataMode(clientRef.current.mode);
      }
      const client = clientRef.current;
      const [ownersData, checkedInData, healthData] = await Promise.all([
        client.getOwners(),
        client.getCheckedIn(),
        client.getHealth()
      ]);
      setOwners(ownersData);
      setCheckedInAnimals(checkedInData);
      setHealth(healthData);
    } catch (loadError) {
      setError(getErrorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }

  async function performAction(action: () => Promise<void>, successMessage?: string) {
    try {
      setBusy(true);
      setError(null);
      setNotice(null);
      await action();
      await loadDashboard();
      if (successMessage) {
        setNotice(successMessage);
      }
    } catch (actionError) {
      setError(getErrorMessage(actionError));
    } finally {
      setBusy(false);
    }
  }

  async function handleRegisterOwner(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await performAction(async () => {
      const client = await getClient();
      await client.registerOwner(ownerForm);
      setOwnerForm(emptyOwnerForm);
    }, "Owner registered.");
  }

  async function handleAddAnimal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await performAction(async () => {
      const client = await getClient();
      await client.addAnimal(animalForm.ownerPhone, {
        name: animalForm.name,
        food: animalForm.food,
        medication: animalForm.medication,
        type: animalForm.type,
        checkedIn: animalForm.checkedIn
      });
      setAnimalForm(emptyAnimalForm);
    }, "Animal registered.");
  }

  async function handleCheckIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await performAction(async () => {
      const client = await getClient();
      await client.checkIn(checkInForm.phone, checkInForm.animalName);
      setCheckInForm(emptyActionForm);
    }, "Animal checked in.");
  }

  async function handleCheckOut(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await performAction(async () => {
      const client = await getClient();
      await client.checkOut(checkOutForm.phone, checkOutForm.animalName);
      setCheckOutForm(emptyActionForm);
    }, "Animal checked out.");
  }

  async function handleTransfer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await performAction(async () => {
      const client = await getClient();
      await client.transfer(
        transferForm.oldOwnerPhone,
        transferForm.animalName,
        transferForm.newOwnerPhone
      );
      setTransferForm(emptyTransferForm);
    }, "Animal transferred.");
  }

  const filteredOwners = owners.filter((owner) => {
    const query = deferredSearch.trim().toLowerCase();
    if (!query) {
      return true;
    }

    const animalNames = owner.animals.map((animal) => animal.name.toLowerCase()).join(" ");
    return (
      owner.name.toLowerCase().includes(query) ||
      owner.phone.toLowerCase().includes(query) ||
      animalNames.includes(query)
    );
  });

  const animalCount = owners.reduce((total, owner) => total + owner.animals.length, 0);
  const actionDisabled = busy;

  return (
    <div className="shell">
      <div className="ambient ambient-a" />
      <div className="ambient ambient-b" />

      <header className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Reception Console Reimagined</p>
          <div className="title-row">
            <h1>Daycare for Animals</h1>
            <img src={`${import.meta.env.BASE_URL}brand-paws.svg`} alt="" className="brand-mark" />
          </div>
        </div>
      </header>

      <main className="layout">
        <section className="panel forms-panel">
          <PanelHeader
            title="Reception Actions"
            subtitle="Register owners, register animals, check in, check out, and transfer."
          />

          {error ? <Banner kind="error" message={error} /> : null}
          {notice ? <Banner kind="success" message={notice} /> : null}

          <div className="form-stack">
            <ActionSection
              title="Register Owner"
              subtitle="Create a new owner profile."
              isOpen={openSection === "register-owner"}
              onToggle={() => toggleSection("register-owner", setOpenSection)}
            >
              <form className="action-form" onSubmit={handleRegisterOwner}>
                <label>
                  <span>Name</span>
                  <input
                    value={ownerForm.name}
                    onChange={(event) =>
                    setOwnerForm((current) => ({ ...current, name: event.target.value }))
                  }
                    placeholder="Jane Doe"
                    required
                  />
                </label>
                <label>
                  <span>Phone</span>
                  <input
                    value={ownerForm.phone}
                    onChange={(event) =>
                      setOwnerForm((current) => ({ ...current, phone: event.target.value }))
                    }
                    placeholder="07012341234"
                    required
                  />
                </label>
                <button disabled={actionDisabled}>Create Owner</button>
              </form>
            </ActionSection>

            <ActionSection
              title="Register Animal"
              subtitle="Add an animal to an existing owner."
              isOpen={openSection === "register-animal"}
              onToggle={() => toggleSection("register-animal", setOpenSection)}
            >
              <form className="action-form" onSubmit={handleAddAnimal}>
                <label>
                  <span>Owner Phone</span>
                  <input
                    value={animalForm.ownerPhone}
                    onChange={(event) =>
                      setAnimalForm((current) => ({ ...current, ownerPhone: event.target.value }))
                    }
                    placeholder="07012341234"
                    required
                  />
                </label>
                <label>
                  <span>Animal Name</span>
                  <input
                    value={animalForm.name}
                    onChange={(event) =>
                    setAnimalForm((current) => ({ ...current, name: event.target.value }))
                  }
                    placeholder="Rex"
                    required
                  />
                </label>
                <label>
                  <span>Food</span>
                  <input
                    value={animalForm.food}
                    onChange={(event) =>
                    setAnimalForm((current) => ({ ...current, food: event.target.value }))
                  }
                    placeholder="Dry food"
                    required
                  />
                </label>
                <label>
                  <span>Medication</span>
                  <input
                    value={animalForm.medication}
                    onChange={(event) =>
                    setAnimalForm((current) => ({ ...current, medication: event.target.value }))
                  }
                    placeholder="None"
                    required
                  />
                </label>
                <label>
                  <span>Type</span>
                  <div className="select-wrap">
                    <select
                      value={animalForm.type}
                      onChange={(event) =>
                        setAnimalForm((current) => ({ ...current, type: event.target.value }))
                      }
                    >
                      <option value="hund">Dog</option>
                      <option value="katt">Cat</option>
                      <option value="fågel">Bird</option>
                    </select>
                  </div>
                </label>
                <label className="checkbox">
                  <input
                    type="checkbox"
                    checked={animalForm.checkedIn}
                    onChange={(event) =>
                      setAnimalForm((current) => ({ ...current, checkedIn: event.target.checked }))
                    }
                  />
                  <span>Register as checked in</span>
                </label>
                <button disabled={actionDisabled}>Add Animal</button>
              </form>
            </ActionSection>

            <ActionSection
              title="Check In"
              subtitle="Mark an animal as present."
              isOpen={openSection === "check-in"}
              onToggle={() => toggleSection("check-in", setOpenSection)}
            >
              <form className="action-form" onSubmit={handleCheckIn}>
                <label>
                  <span>Owner Phone</span>
                  <input
                    value={checkInForm.phone}
                    onChange={(event) =>
                    setCheckInForm((current) => ({ ...current, phone: event.target.value }))
                  }
                    placeholder="0701234567"
                    required
                  />
                </label>
                <label>
                  <span>Animal Name</span>
                  <input
                    value={checkInForm.animalName}
                    onChange={(event) =>
                      setCheckInForm((current) => ({
                        ...current,
                        animalName: event.target.value
                      }))
                    }
                    placeholder="Daisy"
                    required
                  />
                </label>
                <button disabled={actionDisabled}>Check In</button>
              </form>
            </ActionSection>

            <ActionSection
              title="Check Out"
              subtitle="Mark an animal as collected."
              isOpen={openSection === "check-out"}
              onToggle={() => toggleSection("check-out", setOpenSection)}
            >
              <form className="action-form" onSubmit={handleCheckOut}>
                <label>
                  <span>Owner Phone</span>
                  <input
                    value={checkOutForm.phone}
                    onChange={(event) =>
                    setCheckOutForm((current) => ({ ...current, phone: event.target.value }))
                  }
                    placeholder="0701234567"
                    required
                  />
                </label>
                <label>
                  <span>Animal Name</span>
                  <input
                    value={checkOutForm.animalName}
                    onChange={(event) =>
                      setCheckOutForm((current) => ({
                        ...current,
                        animalName: event.target.value
                      }))
                    }
                    placeholder="Texas"
                    required
                  />
                </label>
                <button disabled={actionDisabled}>Check Out</button>
              </form>
            </ActionSection>

            <ActionSection
              title="Transfer Animal"
              subtitle="Move an animal to a different owner."
              isOpen={openSection === "transfer"}
              onToggle={() => toggleSection("transfer", setOpenSection)}
            >
              <form className="action-form" onSubmit={handleTransfer}>
                <label>
                  <span>Current Owner Phone</span>
                  <input
                    value={transferForm.oldOwnerPhone}
                    onChange={(event) =>
                      setTransferForm((current) => ({
                      ...current,
                      oldOwnerPhone: event.target.value
                    }))
                  }
                    placeholder="0701234567"
                  required
                />
              </label>
                <label>
                  <span>Animal Name</span>
                  <input
                    value={transferForm.animalName}
                    onChange={(event) =>
                      setTransferForm((current) => ({
                      ...current,
                      animalName: event.target.value
                    }))
                  }
                    placeholder="Dallas"
                  required
                />
              </label>
                <label>
                  <span>New Owner Phone</span>
                  <input
                    value={transferForm.newOwnerPhone}
                    onChange={(event) =>
                      setTransferForm((current) => ({
                      ...current,
                      newOwnerPhone: event.target.value
                    }))
                  }
                    placeholder="0709876543"
                  required
                />
              </label>
                <button disabled={actionDisabled}>Transfer</button>
              </form>
            </ActionSection>
          </div>
        </section>

        <section className="panel board-panel">
          <PanelHeader
            title="Live Board"
            subtitle="Search owners and review all animals against the same backend state the CLI uses."
          />

          <div className="status-grid board-status-grid">
            <StatCard label="Owners" value={String(owners.length)} />
            <StatCard label="Animals" value={String(animalCount)} />
            <StatCard label="Checked In" value={String(checkedInAnimals.length)} />
            <StatCard
              label="Mode"
              value={dataMode === "api" ? "Java API" : dataMode === "browser" ? "Browser" : "Unknown"}
              accent
            />
          </div>

          {dataMode === "browser" ? (
            <div className="backend-alert backend-alert-info">
              <strong>Demo mode is active.</strong>
              <p>
                Your data is stored in this browser with local storage. If someone downloads the
                repo and runs the Java backend locally, the app will automatically use that instead.
              </p>
            </div>
          ) : null}

          <div className="toolbar">
            <input
              className="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by owner, phone, or animal name"
            />
            <button className="ghost" onClick={() => void loadDashboard()} disabled={loading || busy}>
              Refresh
            </button>
          </div>

          <div className="checked-in-strip">
            <div>
              <p className="strip-label">Currently Checked In</p>
              <strong>{checkedInAnimals.length} animal(s)</strong>
            </div>
            <div className="checked-in-list">
              {checkedInAnimals.length === 0 ? (
                <span className="chip muted">No animals checked in</span>
              ) : (
                checkedInAnimals.map((entry) => (
                  <span className="chip" key={`${entry.ownerPhone}-${entry.animal.name}`}>
                    {entry.animal.name} · {entry.ownerName}
                  </span>
                ))
              )}
            </div>
          </div>

          {loading ? <p className="empty-state">Loading reception board...</p> : null}
          {!loading && filteredOwners.length === 0 ? (
            <p className="empty-state">No owners match the current search.</p>
          ) : null}

          <div className="owner-directory">
            {filteredOwners.map((owner) => (
              <OwnerSection
                key={owner.phone}
                owner={owner}
                isOpen={openOwnerPhone === owner.phone}
                onToggle={() => toggleOwner(owner.phone, setOpenOwnerPhone)}
              />
            ))}
          </div>
        </section>
      </main>

      <footer className="site-footer">
        <section className="footer-card">
          <span className="footer-label">About</span>
          <div className="footer-routes">
            <a href="#">About</a>
            <a href="#">Contact</a>
            <a href="#">Privacy</a>
            <a href="#">Terms</a>
          </div>
        </section>
        <section className="footer-card">
          <span className="footer-label">Contributors</span>
          <div className="footer-contributors">
            <a href="https://github.com/alpsten" target="_blank" rel="noreferrer">Emil</a>
            <a href="https://github.com/CarolinaFromm" target="_blank" rel="noreferrer">Carolina</a>
            <a href="https://github.com/Dannyyari" target="_blank" rel="noreferrer">Danny</a>
            <a href="https://github.com/RobinOqvist" target="_blank" rel="noreferrer">Robin</a>
            <a href="https://github.com/TildeHv" target="_blank" rel="noreferrer">Tilde</a>
          </div>
        </section>
      </footer>
    </div>
  );

  async function getClient() {
    if (!clientRef.current) {
      clientRef.current = await getDataClient();
      setDataMode(clientRef.current.mode);
    }
    return clientRef.current;
  }
}

function StatCard({ label, value, accent = false }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className={accent ? "stat-card stat-card-accent" : "stat-card"}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function PanelHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="panel-header">
      <h2>{title}</h2>
      <p>{subtitle}</p>
    </div>
  );
}

function ActionSection({
  title,
  subtitle,
  isOpen,
  onToggle,
  children
}: {
  title: string;
  subtitle: string;
  isOpen: boolean;
  onToggle: () => void;
  children: React.ReactNode;
}) {
  return (
    <section className={isOpen ? "action-card action-card-open" : "action-card"}>
      <button type="button" className="action-toggle" onClick={onToggle}>
        <span>
          <strong>{title}</strong>
          <small>{subtitle}</small>
        </span>
        <span className={isOpen ? "action-chevron action-chevron-open" : "action-chevron"}>
          ▾
        </span>
      </button>
      {isOpen ? <div className="action-body">{children}</div> : null}
    </section>
  );
}

function OwnerSection({
  owner,
  isOpen,
  onToggle
}: {
  owner: Owner;
  isOpen: boolean;
  onToggle: () => void;
}) {
  return (
    <article className={isOpen ? "owner-card owner-card-open" : "owner-card"}>
      <div className="owner-toggle">
        <div className="owner-toggle-copy">
          <button type="button" className="owner-name-button" onClick={onToggle}>
            {owner.name}
          </button>
          <p className="owner-phone-copy" title="Select and copy the phone number">
            {owner.phone}
          </p>
        </div>
        <div className="owner-toggle-meta">
          <span className="badge">{owner.animals.length} animals</span>
          <button type="button" className="owner-expand-button" onClick={onToggle} aria-label="Toggle owner profile">
            <span className={isOpen ? "action-chevron action-chevron-open" : "action-chevron"}>▾</span>
          </button>
        </div>
      </div>

      {isOpen ? (
        <div className="owner-body">
          {owner.animals.length === 0 ? (
            <p className="muted-copy">No registered animals yet.</p>
          ) : (
            <div className="animal-list">
              {owner.animals.map((animal) => (
                <div className="animal-row" key={`${owner.phone}-${animal.name}`}>
                  <div>
                    <strong>{animal.name}</strong>
                    <p>
                      {animal.type} · Food: {animal.food} · Medication: {animal.medication}
                    </p>
                  </div>
                  <span className={animal.checkedIn ? "state active" : "state idle"}>
                    {animal.checkedIn ? "In" : "Out"}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}
    </article>
  );
}

function Banner({ kind, message }: { kind: "error" | "success"; message: string }) {
  return <div className={kind === "error" ? "banner banner-error" : "banner banner-success"}>{message}</div>;
}

function toggleSection(
  id: ActionSectionId,
  setOpenSection: React.Dispatch<React.SetStateAction<ActionSectionId | null>>
) {
  setOpenSection((current) => (current === id ? null : id));
}

function toggleOwner(
  phone: string,
  setOpenOwnerPhone: React.Dispatch<React.SetStateAction<string | null>>
) {
  setOpenOwnerPhone((current) => (current === phone ? null : phone));
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "Something went wrong while talking to the API.";
}
