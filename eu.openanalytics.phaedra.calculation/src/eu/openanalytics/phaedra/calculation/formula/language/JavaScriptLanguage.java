package eu.openanalytics.phaedra.calculation.formula.language;

import java.util.Arrays;
import java.util.Map;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.calculation.formula.FormulaUtils;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class JavaScriptLanguage extends BaseLanguage {

	public static final String ID = "javaScript";
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getLabel() {
		return "JavaScript";
	}

	@Override
	protected Map<String, Object> buildContext(CalculationFormula formula, IValueObject inputValue, IFeature feature) {
		Map<String, Object> context = super.buildContext(formula, inputValue, feature);
		context.put("feature", feature);
		
		InputType type = FormulaUtils.getInputType(formula);
		if (inputValue instanceof Well) {
			Well well = (Well) inputValue;
			context.put("plate", well.getPlate());
			context.put("well", well);			
		} else if (inputValue instanceof Plate) {
			Plate plate = (Plate) inputValue;
			context.put("plate", plate);
			Well[] wells = PlateService.streamableList(plate.getWells())
					.stream()
					.sorted(PlateUtils.WELL_NR_SORTER)
					.toArray(i -> new Well[i]);
			double[] inputValues = Arrays.stream(wells).mapToDouble(w -> type.getInputValue(w, feature)).toArray();
			context.put("wells", wells);
			context.put("outputValues", new double[inputValues.length]);
		}
		
		return context;
	}

	@Override
	public void transformFormulaOutput(IValueObject inputValue, Object outputValue, CalculationFormula formula, Map<String, Object> context, double[] outputArray) {
		switch (FormulaUtils.getScope(formula)) {
		case PerWell:
			Well well = (Well) inputValue;
			int index = PlateUtils.getWellNr(well) - 1;
			outputArray[index] = getAsDouble(outputValue);
			break;
		case PerPlate:
		default:
			double[] outputValues = (double[]) context.get("outputValues");
			for (index=0; index<outputArray.length; index++) {
				outputArray[index] = outputValues[index];
			}
		}
	}
}