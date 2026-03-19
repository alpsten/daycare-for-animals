package daycare.storage;

import daycare.model.Owner;

import java.util.List;

public interface OwnerStorage {
    List<Owner> loadOwners();

    void saveOwners(List<Owner> owners);

    void appendOwner(Owner owner);
}
