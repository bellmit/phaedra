package eu.openanalytics.phaedra.ui.plate.util;

import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateSummary;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;


public class PlateSummaryWithStats extends PlateSummary {
	
	public static PlateSummaryWithStats loadSummary(Plate item) {
		PlateSummary summary = PlateService.getInstance().getPlateSummary(item);
		StatService.getInstance().loadPersistentPlateStats(item);
		return new PlateSummaryWithStats(item, summary);
	}
	
	
	private final Plate plate;
	
	
	public PlateSummaryWithStats(Plate plate, PlateSummary summary) {
		this.plate = plate;
		this.crcCount = summary.crcCount;
		this.screenCount = summary.screenCount;
	}
	
	@Override
	public double getStat(String stat, Feature f, String wellType, String norm) {
		return StatService.getInstance().calculate(stat, plate, f, wellType, norm);
	}
	
}
