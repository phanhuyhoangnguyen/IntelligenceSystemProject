package EnergyAgents;

public interface EnergyGenerator {
	public double getActualEnergyProduction(String startTime, String endTime);
	public double getDefaultEnergProduction();
	public String updateActualEnergyProductionToDatabase();
}