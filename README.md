# Daycare for Animals

Daycare for Animals is a continued and polished version of a school group project. What started as a Java console application for managing animal daycare reception tasks has been expanded into a small multi-surface app with a reusable Java core, a local CLI, a thin Java HTTP API, and a TypeScript frontend.

The core business logic is still the same: owners can be registered, animals can be added to owners, animals can be checked in and out, and ownership can be transferred. The main improvement is the structure around that logic. The Java side is now packaged by responsibility, the storage format is JSON-based, and the frontend can run either against the Java backend or in browser-only demo mode through local storage.

## About

This project is a reception desk for an animal daycare. It is designed to preserve the original school assignment logic while making the app easier to run, present, and continue developing.

Today the project includes:

- a Java CLI for the original local console workflow
- a reusable Java service layer that owns the business rules
- a Java API layer for frontend integration
- a TypeScript frontend for a cleaner, more accessible demo experience
- browser `localStorage` support for online demos such as GitHub Pages

That makes the project usable in two ways:

- locally, with the Java backend as the authoritative source of truth
- online, as a lightweight frontend demo that stores data in the browser

## Project Structure

```text
src/
  daycare/
    api/
    cli/
    model/
    service/
    storage/
    util/
  owners.txt

web/
  src/
  public/
```

## Run Locally

Compile the Java sources:

```bash
javac $(find src -name '*.java')
```

Run the original CLI:

```bash
java -cp src daycare.cli.Reception
```

Run the Java API:

```bash
java -cp src daycare.api.DaycareApiServer 8080
```

Run the TypeScript frontend:

```bash
cd web
npm install
npm run dev
```

## Frontend Modes

The frontend supports two modes:

- `api` mode: talks to the Java backend
- `browser` mode: stores data in browser `localStorage`

If the Java API is not reachable, the frontend falls back to browser mode automatically. This makes GitHub Pages deployment possible without hosting Java separately

## Contributors

Current project contributors:

- Carolina
- Danny
- Robin
- Tilde

## TODO

- Add real GitHub profile links for Carolina, Danny, Robin, and Tilde in the frontend footer and in this README.
- Replace the placeholder footer routes with real About, Contact, Privacy, and Terms pages or sections.
- Add a proper About section in the frontend instead of a placeholder link.
- Add a small README section for deployment steps to GitHub Pages.
- Add automated tests for the Java service layer and key frontend flows.
