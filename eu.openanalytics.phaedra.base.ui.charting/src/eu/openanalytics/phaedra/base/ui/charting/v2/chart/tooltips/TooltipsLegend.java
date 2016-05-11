package eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class TooltipsLegend<ENTITY, ITEM> extends AbstractLegend<ENTITY, ITEM> {

	@Override
	public List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems() {
		return null;
	}

	@Override
	public void showSettingsDialog(Shell shell, ValueObservable observable) {
		new TooltipSettingsDialog<>(shell, getLayer()).open();
	}

}