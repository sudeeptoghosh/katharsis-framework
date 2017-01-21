package io.katharsis.validation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.Test;

import io.katharsis.core.internal.utils.StringUtils;
import io.katharsis.queryParams.QueryParams;
import io.katharsis.validation.internal.ConstraintViolationImpl;
import io.katharsis.validation.mock.ComplexValidator;
import io.katharsis.validation.mock.models.Project;
import io.katharsis.validation.mock.models.ProjectData;
import io.katharsis.validation.mock.models.Task;

// TODO remo: root/leaf bean not yet available, Katharsis extensions required
public class ValidationTest extends AbstractValidationTest {

	@Test
	public void testPropertyNotNull() {
		Project project = new Project();
		project.setId(1L);
		project.setName(null); // violation
		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{javax.validation.constraints.NotNull.message}", violation.getMessageTemplate());
			Assert.assertEquals("name", violation.getPropertyPath().toString());
			Assert.assertEquals("data/attributes/name", violation.getErrorData().getSourcePointer());
		}
	}

	@Test
	public void testListAttribute() {
		Project project = new Project();
		project.setId(1L);
		project.setName("test");
		project.getKeywords().add("1");
		project.getKeywords().add("2");
		project.getKeywords().add("3");
		project.getKeywords().add("4");
		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{javax.validation.constraints.Size.message}", violation.getMessageTemplate());
			Assert.assertEquals("keywords", violation.getPropertyPath().toString());
			Assert.assertEquals("data/attributes/keywords", violation.getErrorData().getSourcePointer());
		}
	}

	@Test
	public void testNestedPropertyNotNull() {
		ProjectData data = new ProjectData();
		data.setValue(null); // violation

		Project project = new Project();
		project.setId(1L);
		project.setName("test");
		project.setData(data);

		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{javax.validation.constraints.NotNull.message}", violation.getMessageTemplate());
			Assert.assertEquals("data.value", violation.getPropertyPath().toString());
			Assert.assertEquals("data/attributes/data/value", violation.getErrorData().getSourcePointer());
		}
	}

	@Test
	public void testListElementAttributeNotNull() {
		ProjectData data = new ProjectData();
		data.setValue(null); // violation

		Project project = new Project();
		project.setId(1L);
		project.setName("test");
		project.getDataList().add(data);

		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{javax.validation.constraints.NotNull.message}", violation.getMessageTemplate());
			Assert.assertEquals("dataList[0].value", violation.getPropertyPath().toString());
			Assert.assertEquals("data/attributes/dataList/0/value", violation.getErrorData().getSourcePointer());
		}
	}

	@Test
	public void testMapElementAttributeNotNull() {
		ProjectData data = new ProjectData();
		data.setValue(null); // violation

		Project project = new Project();
		project.setId(1L);
		project.setName("test");
		project.setDataMap(new LinkedHashMap());
		project.getDataMap().put("someKey", data);

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		System.out.println(validator.validate(project));
		
		
		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{javax.validation.constraints.NotNull.message}", violation.getMessageTemplate());
			Assert.assertEquals("dataMap[someKey].value", violation.getPropertyPath().toString());
			Assert.assertEquals("data/attributes/dataMap/someKey/value", violation.getErrorData().getSourcePointer());
		}
	}

	@Test
	public void testSetElementAttributeNotNull() {
		Project project = new Project();
		project.setId(1L);
		project.setName("test");
		// ProjectData corrupedElement = null;
		for (int i = 0; i < 11; i++) {
			ProjectData data = new ProjectData();
			if (i != 3) {
				data.setValue(Integer.toString(i));
				// corrupedElement = data;
			}
			project.getDataSet().add(data);
		}

		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{javax.validation.constraints.NotNull.message}", violation.getMessageTemplate());
			Assert.assertTrue(violation.getPropertyPath().toString().startsWith("dataSet["));
			Assert.assertTrue(violation.getPropertyPath().toString().endsWith("].value"));

			Assert.assertTrue(violation.getErrorData().getSourcePointer().startsWith("data/attributes/dataSet/"));

			//	TODO attempt to preserver order in Katharsis by comparing incoming request, sourcePointer and server Set
			//  or use of order preserving sets
			//			List<ProjectData> list = new ArrayList<>(project.getDataSet());
			//			int index = list.indexOf(corrupedElement);
			//			Assert.assertEquals(violation.getErrorData().getSourcePointer(), "data/attributes/dataSet/" + index + "/value");
		}
	}

	@Test
	public void testResourceObjectValidation() {
		Project project = new Project();
		project.setId(1L);
		project.setName(ComplexValidator.INVALID_NAME);
		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{complex.message}", violation.getMessageTemplate());
			Assert.assertEquals("", violation.getPropertyPath().toString());
			Assert.assertEquals("", violation.getErrorData().getSourcePointer());
		}
	}

	@Test
	public void testValidationException() {
		Project project = new Project();
		project.setId(1L);
		// trigger ValidationException
		project.setName(ValidationException.class.getSimpleName());
		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ValidationException e) {
			Assert.assertEquals("messageKey", e.getMessage());
		}
	}

	@Test
	public void testPropertyOnRelation() {
		Task task = new Task();
		task.setId(1L);
		task.setName(null);
		taskRepo.create(task);

		Project project = new Project();
		project.setId(2L);
		project.setName("test");
		project.getTasks().add(task);

		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{javax.validation.constraints.NotNull.message}", violation.getMessageTemplate());
			Assert.assertEquals("tasks[0]", violation.getPropertyPath().toString());
			Assert.assertEquals("data/relationships/tasks/0", violation.getErrorData().getSourcePointer());
		}
	}

	@Test
	public void testRelationProperty() {
		Task task = new Task();
		task.setId(1L);
		task.setName(ComplexValidator.INVALID_NAME);
		taskRepo.create(task);

		Project project = new Project();
		project.setName("test");
		project.setTask(task);

		try {
			projectRepo.create(project);
			Assert.fail();
		}
		catch (ConstraintViolationException e) {
			Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
			Assert.assertEquals(1, violations.size());
			ConstraintViolationImpl violation = (ConstraintViolationImpl) violations.iterator().next();
			Assert.assertEquals("{complex.message}", violation.getMessageTemplate());
			Assert.assertEquals("task", violation.getPropertyPath().toString());
			Assert.assertEquals("data/relationships/task", violation.getErrorData().getSourcePointer());
		}
	}

	private QueryParams buildIncludes(String root, String... relations) {
		Map<String, Set<String>> params = new HashMap<String, Set<String>>();
		addParams(params, "include[" + root + "]", StringUtils.join(",", Arrays.asList(relations)));
		return queryParamsBuilder.buildQueryParams(params);
	}

	private void addParams(Map<String, Set<String>> params, String key, String value) {
		params.put(key, new HashSet<String>(Arrays.asList(value)));
	}

}
