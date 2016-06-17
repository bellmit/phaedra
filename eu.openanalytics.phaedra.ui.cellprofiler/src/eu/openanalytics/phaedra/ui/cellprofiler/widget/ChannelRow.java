package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.imaging.util.TIFFCodec;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

/**
 * TODO Montage, pattern groups
 */
class ChannelRow extends Composite {

	private ChannelComposer composer;
	private ImageChannel channel;
	
	private Label sequenceLbl;
	private Text nameTxt;
	private ColorSelector colorMaskBtn;
	private Label patternLbl;
	
	public ChannelRow(ChannelComposer composer, ImageChannel channel) {
		super(composer.getChannelRowArea(), SWT.BORDER);
		this.composer = composer;
		this.channel = channel;
		GridLayoutFactory.fillDefaults().numColumns(9).spacing(0, 0).applyTo(this);
		
		sequenceLbl = new Label(this, SWT.NONE);
		GridDataFactory.fillDefaults().hint(15, SWT.DEFAULT).indent(5, 0).align(SWT.BEGINNING, SWT.CENTER).applyTo(sequenceLbl);
		
		Button moveUpBtn = new Button(this, SWT.PUSH);
		moveUpBtn.setImage(IconManager.getIconImage("arrow_up.png"));
		moveUpBtn.setToolTipText("Move channel up");
		moveUpBtn.addListener(SWT.Selection, e -> composer.moveChannelUp(channel));
		
		Button moveDownBtn = new Button(this, SWT.PUSH);
		moveDownBtn.setImage(IconManager.getIconImage("arrow_down.png"));
		moveDownBtn.setToolTipText("Move channel down");
		moveDownBtn.addListener(SWT.Selection, e -> composer.moveChannelDown(channel));
		
		nameTxt = new Text(this, SWT.BORDER);
		nameTxt.addModifyListener(e -> channel.setName(nameTxt.getText()));
		GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(nameTxt);
		
		colorMaskBtn = new ColorSelector(this);
		colorMaskBtn.addListener(e -> channel.setColorMask(ColorUtils.rgbToHex(colorMaskBtn.getColorValue())));
		GridDataFactory.fillDefaults().indent(5, 0).applyTo(colorMaskBtn.getButton());
		
		patternLbl = new Label(this, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).align(SWT.FILL, SWT.CENTER).indent(5, 0).applyTo(patternLbl);
		
		Button editPatternBtn = new Button(this, SWT.PUSH);
		editPatternBtn.setText("...");
		editPatternBtn.setToolTipText("Edit pattern");
		editPatternBtn.addListener(SWT.Selection, e -> editPattern());
		
		Button previewBtn = new Button(this, SWT.PUSH);
		previewBtn.setImage(IconManager.getIconImage("image.png"));
		previewBtn.setToolTipText("Preview");
		previewBtn.addListener(SWT.Selection, e -> previewImage());
		
		Button removeBtn = new Button(this, SWT.PUSH);
		removeBtn.setImage(IconManager.getIconImage("delete.png"));
		removeBtn.setToolTipText("Remove channel");
		removeBtn.addListener(SWT.Selection, e -> composer.removeChannel(channel));
		
		refresh();
	}

	public ImageChannel getChannel() {
		return channel;
	}
	
	public void refresh() {
		sequenceLbl.setText("" + channel.getSequence());
		nameTxt.setText(channel.getName());
		colorMaskBtn.setColorValue(ColorUtils.hexToRgb(channel.getColorMask()));
		patternLbl.setText(channel.getDescription());
	}
	
	private void editPattern() {
		new EditPatternDialog(getShell(), channel, composer.getImageFolder()).open();
		refresh();
	}
	
	private void previewImage() {
		if (channel.getDescription() == null || channel.getDescription().isEmpty()) return;
		Path sampleFilePath = null;
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(composer.getImageFolder())) {
			Pattern pattern = Pattern.compile(channel.getDescription());
            for (Path path : directoryStream) {
            	Matcher matcher = pattern.matcher(path.getFileName().toString());
            	if (matcher.matches()) {
            		sampleFilePath = path;
            		break;
            	}
            }
		} catch (PatternSyntaxException e) {
			MessageDialog.openError(getShell(), "Cannot Preview", "Invalid pattern: " + channel.getDescription());
			return;
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Cannot Preview", "Failed to locate sample image: " + e.getMessage());
			return;
		}
		if (sampleFilePath == null) {
			MessageDialog.openError(getShell(), "Cannot Preview", "No image found matching the pattern: " + channel.getDescription());
			return;
		}
		
		try {
			String ext = FileUtils.getExtension(sampleFilePath.getFileName().toString());
			String fullPath = sampleFilePath.toFile().getAbsolutePath();
			ImageData imageData = ext.equalsIgnoreCase("tif") ? TIFFCodec.read(fullPath)[0] : new ImageLoader().load(fullPath)[0];
			
			int actualDepth = imageData.depth == 24 ? 8 : imageData.depth;
			int maxLevel = (int) Math.pow(2, actualDepth) - 1;
			channel.setBitDepth(actualDepth);
			channel.setLevelMin(0);
			if (channel.getLevelMax() == 0 || channel.getLevelMax() > maxLevel) channel.setLevelMax(maxLevel);
			
			new ImagePreviewDialog(getShell(), channel, imageData).open();
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Cannot preview image", "Failed to create preview: " + e.getMessage());
		}
	}
}