#ifndef WISHBONE_GEN_BUS_DESCRIPTION_INCLUDED
#define WISHBONE_GEN_BUS_DESCRIPTION_INCLUDED

#include <string>
#include <vector>

/**
 * Describe a master component
*/
class MasterComponent {
public:
    MasterComponent(std::string name);

    std::string getName();

private:
    std::string name;
};

/**
 * Describe a slave component
*/
class SlaveComponent {
public:
    SlaveComponent(std::string name, int start_address, int size);

    std::string getName();

    int getStartAddress();

    int getSize();

private:
    std::string name;
    int start_address;
    int size;
};


/**
 * Contain all the informations required to generate the bus
*/
class BusDescription {
public:

    /**
     * Parse the given XML file to generate a BusDescription
    */
    BusDescription(std::string filename);

    /**
     * Indicate if the Bus require an error signal
    */
    bool requireError();
    /**
     * Indicate if the Bus require a retry signal
    */
    bool requireRetry();
    
    /**
     * Get the list of master components
    */
    const std::vector<MasterComponent>& getMasters();
    
    /**
     * Get the list of slave components
    */
    const std::vector<SlaveComponent>& getSlaves();

private:
    // --- Options ---
    bool error;
    bool retry;
    // std::string bus_type

    // --- Master components ---
    std::vector<MasterComponent> masters;
    
    // --- Memory Regions / Slave components ---
    std::vector<SlaveComponent> slaves;
};

#endif