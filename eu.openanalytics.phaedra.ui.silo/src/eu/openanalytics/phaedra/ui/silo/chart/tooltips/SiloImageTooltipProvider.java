package eu.openanalytics.phaedra.ui.silo.chart.tooltips;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.TooltipsSettings;
import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import uk.ac.starlink.ttools.plot.Tooltip;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public class SiloImageTooltipProvider implements ITooltipProvider {

	public static final String CFG_SCALE = "scale";
	public static final String CFG_CHANNELS = "channels";

	public static final float DEFAULT_SCALE = 1/2f;
	public static final boolean[] DEFAULT_CHANNELS = null;

	private SiloDataProvider dataProvider;
	private float[] cellIndex;

	private float scale = DEFAULT_SCALE;
	private boolean[] channels = DEFAULT_CHANNELS;

	public SiloImageTooltipProvider(SiloDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Override
	public void starting() {
		cellIndex = dataProvider.getCellIndexColumn();
	}

	@Override
	public Point getTooltipSize(int index) {
		Well w = SelectionUtils.getAsClass(dataProvider.getRowObject(index), Well.class);
		if (cellIndex != null && cellIndex.length > index) {
			int currentCellIndex = (int) cellIndex[index];
			Rectangle rect = ImageRenderService.getInstance().getSubWellImageBounds(w, currentCellIndex, scale);
			return new Point(rect.width, rect.height);
		} else {
			org.eclipse.swt.graphics.Point size = ImageRenderService.getInstance().getWellImageSize(w, scale);
			return new Point(size.x, size.y);
		}
	}

	@Override
	public Tooltip getTooltip(int index) throws IOException {
		Well w = SelectionUtils.getAsClass(dataProvider.getRowObject(index), Well.class);

		ImageSettings currentSettings = PlateUtils.getProtocolClass(w).getImageSettings();

		List<ImageChannel> imageChannels = currentSettings.getImageChannels();
		if (channels == null || imageChannels.size() != channels.length) {
			channels = new boolean[imageChannels.size()];
			for (int i = 0; i < imageChannels.size(); i++) {
				channels[i] = imageChannels.get(i).isShowInWellView();
			}
		}

		if (cellIndex != null && cellIndex.length > index) {
			int currentCellIndex = (int) cellIndex[index];
			ImageData imageData = ImageRenderService.getInstance().getSubWellImageData(w, currentCellIndex, scale, channels);
			Image tmpImage = new Image(null, imageData);
			try {
				BufferedImage awtImage = AWTImageConverter.convert(tmpImage);
				return new Tooltip(awtImage, "Cell " + currentCellIndex + " @ Well " + PlateUtils.getWellCoordinate(w));
			} finally {
				tmpImage.dispose();
			}
		} else {
			ImageData imageData = ImageRenderService.getInstance().getWellImageData(w, scale, channels);
			Image tmpImage = new Image(null, imageData);
			try {
				BufferedImage awtImage = AWTImageConverter.convert(tmpImage);
				return new Tooltip(awtImage, "Well " + PlateUtils.getWellCoordinate(w));
			} finally {
				tmpImage.dispose();
			}
		}
	}

	@Override
	public void setConfig(Object config) {
		if (config instanceof TooltipsSettings) {
			Object obj = ((TooltipsSettings) config).getMiscSetting(CFG_SCALE);
			if (obj instanceof Float) {
				this.scale = (float) obj;
			} else {
				this.scale = DEFAULT_SCALE;
			}
			obj = ((TooltipsSettings) config).getMiscSetting(CFG_CHANNELS);
			if (obj instanceof boolean[]) {
				this.channels = (boolean[]) obj;
			} else {
				this.channels = DEFAULT_CHANNELS;
			}
		}
	}

	@Override
	public void dispose() {
		// Do nothing.
	}

}