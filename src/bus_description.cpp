#include <bus_description.hpp>

#include <pugixml.hpp>

MasterComponent::MasterComponent(std::string name): name(name) {}

std::string MasterComponent::getName() { return name; }

SlaveComponent::SlaveComponent(std::string name, int start_address, int size):
    name(name), start_address(start_address), size(size) {}

std::string SlaveComponent::getName() { return name; }

int SlaveComponent::getStartAddress() { return start_address; }

int SlaveComponent::getSize() { return size; }

BusDescription::BusDescription(std::string filename) {
    // TODO

    // Read XML file
    pugi::xml_document doc;
    pugi::xml_parse_result result = doc.load_file(filename.c_str());

    // Complete options

    // Add Masters

    // Add Slaves
}

bool BusDescription::requireError() {
    return error;
} 

bool BusDescription::requireRetry() {
    return retry;
} 

const std::vector<MasterComponent>& BusDescription::getMasters() {
    return masters;
}

const std::vector<SlaveComponent>& BusDescription::getSlaves() {
    return slaves;
}