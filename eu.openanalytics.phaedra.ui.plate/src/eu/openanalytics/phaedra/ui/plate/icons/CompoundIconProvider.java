package eu.openanalytics.phaedra.ui.plate.icons;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.Compound;

public class CompoundIconProvider extends AbstractIconProvider<Compound> {
	@Override
	public Class<Compound> getType() {
		return Compound.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("benzene.gif");
	}

	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return null;
	}

}
