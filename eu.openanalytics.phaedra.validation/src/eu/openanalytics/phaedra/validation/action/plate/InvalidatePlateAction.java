package eu.openanalytics.phaedra.validation.action.plate;

import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;


public class InvalidatePlateAction extends AbstractPlateAction {

	@Override
	protected PlateValidationStatus getActionValidationStatus() {
		return PlateValidationStatus.INVALIDATED;
	}
}
