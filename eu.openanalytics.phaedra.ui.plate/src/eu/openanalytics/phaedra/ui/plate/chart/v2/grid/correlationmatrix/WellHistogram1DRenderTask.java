package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram.Histogram1DChart;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellHistogram1DRenderTask extends WellChartRenderTask{

	public WellHistogram1DRenderTask(List<Well> wells, List<String> features, String fillOption,
			DataUnitConfig dataUnitConfig,
			int w, int h, int row, int col) {
		super(wells, features, fillOption, dataUnitConfig, w, h, row, col);
	}

	@Override
	public AbstractChart<Plate, Well> getChart() {
		return new Histogram1DChart<Plate, Well>();
	}
}
