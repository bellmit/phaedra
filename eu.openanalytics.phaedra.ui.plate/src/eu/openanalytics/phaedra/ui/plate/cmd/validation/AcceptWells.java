package eu.openanalytics.phaedra.ui.plate.cmd.validation;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;
import eu.openanalytics.phaedra.validation.ValidationUtils;


public class AcceptWells extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
		
		execute(wells);
		return null;
	}

	public static void execute(final List<Well> wells) {
		if (!wells.isEmpty()) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					ValidationUtils.clearRejectReason(wells);
				}
			};
			ValidationJobHelper.doInJob(Action.ACCEPT_WELL, null, r, wells);
		}
	}
}