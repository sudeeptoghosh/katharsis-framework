package io.katharsis.resource.internal;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.katharsis.errorhandling.ErrorData;
import io.katharsis.jackson.JsonApiModuleBuilder;
import io.katharsis.locator.SampleJsonServiceLocator;
import io.katharsis.module.ModuleRegistry;
import io.katharsis.resource.Document;
import io.katharsis.resource.Relationship;
import io.katharsis.resource.Resource;
import io.katharsis.resource.ResourceId;
import io.katharsis.resource.field.ResourceFieldNameTransformer;
import io.katharsis.resource.information.AnnotationResourceInformationBuilder;
import io.katharsis.resource.information.ResourceInformationBuilder;
import io.katharsis.resource.registry.ConstantServiceUrlProvider;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.resource.registry.ResourceRegistryBuilder;
import io.katharsis.resource.registry.ResourceRegistryBuilderTest;
import io.katharsis.resource.registry.ResourceRegistryTest;

public class DocumentSerializerTest {

	private ObjectMapper objectMapper;

	private ObjectReader reader;

	private ObjectWriter writer;

	@Before
	public void setup() {
		ResourceInformationBuilder resourceInformationBuilder = new AnnotationResourceInformationBuilder(new ResourceFieldNameTransformer());
		ResourceRegistryBuilder registryBuilder = new ResourceRegistryBuilder(new SampleJsonServiceLocator(), resourceInformationBuilder);
		ResourceRegistry resourceRegistry = registryBuilder.build(ResourceRegistryBuilderTest.TEST_MODELS_PACKAGE, new ModuleRegistry(), new ConstantServiceUrlProvider(ResourceRegistryTest.TEST_MODELS_URL));

		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JsonApiModuleBuilder().build(resourceRegistry, false));
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		reader = objectMapper.reader().forType(Document.class);
		writer = objectMapper.writer();
	}

	@Test
	public void testSingleResource() throws JsonProcessingException, IOException {
		Document doc = new Document();
		Resource resource = new Resource();
		resource.setId("2");
		resource.setType("tasks");
		resource.setAttribute("name", objectMapper.readTree("\"sample task\""));
		doc.setData(resource);

		String json = writer.writeValueAsString(doc);

		StringBuilder expected = new StringBuilder();
		expected.append("{");
		expected.append("\"data\" : {");
		expected.append("  \"id\" : \"2\",");
		expected.append("  \"type\" : \"tasks\",");
		expected.append("  \"attributes\" : {");
		expected.append("    \"name\" : \"sample task\"");
		expected.append("    }");
		expected.append("  }");
		expected.append("}");
		expected.append("}");

		assertThatJson(json).describedAs(expected.toString());

		Document readDoc = reader.readValue(json);
		Assert.assertEquals(doc, readDoc);
	}

	@Test
	public void testInformation() throws JsonProcessingException, IOException {
		Document doc = new Document();
		doc.setMeta((ObjectNode) objectMapper.readTree("{\"metaName\" : \"metaValue\"}"));
		doc.setLinks((ObjectNode) objectMapper.readTree("{\"linkName\" : \"linkValue\"}"));

		String json = writer.writeValueAsString(doc);

		StringBuilder expected = new StringBuilder();
		expected.append("{");
		expected.append("  'meta' : {'metaName' = 'metaValue'},");
		expected.append("  'links' : {'linkName' = 'linkValue'}");
		expected.append("}");

		assertThatJson(json).describedAs(expected.toString().replace('\'', '\"'));

		Document readDoc = reader.readValue(json);
		Assert.assertEquals(doc, readDoc);
	}

	@Test
	public void testErrors() throws JsonProcessingException, IOException {
		Document doc = new Document();

		ErrorData error = ErrorData.builder().setCode("test").build();
		doc.setErrors(Arrays.asList(error));

		String json = writer.writeValueAsString(doc);

		StringBuilder expected = new StringBuilder();
		expected.append("{");
		expected.append("  'errors' : [");
		expected.append("    '{");
		expected.append("       'code' = 'test'");
		expected.append("    '}");
		expected.append("  ]");
		expected.append("}");

		assertThatJson(json).describedAs(expected.toString().replace('\'', '\"'));

		Document readDoc = reader.readValue(json);
		Assert.assertEquals(doc, readDoc);
	}

	@Test
	public void testSingleValuedRelationship() throws JsonProcessingException, IOException {
		Document doc = new Document();
		Resource resource = new Resource();
		resource.setId("2");
		resource.setType("tasks");
		Relationship relationship = new Relationship();
		relationship.setData(new ResourceId("3", "projects"));
		relationship.setMeta((ObjectNode) objectMapper.readTree("{\"metaName\" : \"metaValue\"}"));
		relationship.setLinks((ObjectNode) objectMapper.readTree("{\"linkName\" : \"linkValue\"}"));
		resource.getRelationships().put("project", relationship);
		doc.setData(resource);

		String json = writer.writeValueAsString(doc);

		StringBuilder expected = new StringBuilder();
		expected.append("{");
		expected.append("'data' : {");
		expected.append("  'id' : '2',");
		expected.append("  'type' : 'tasks',");
		expected.append("  'relationships' : {");
		expected.append("    'project' : {");
		expected.append("       'data' : {'id' = '3', 'type' = 'projects'},");
		expected.append("       'meta' : {'metaName' = 'metaValue'},");
		expected.append("       'links' : {'linkName' = 'linkValue'}");
		expected.append("    }");
		expected.append("  }");
		expected.append("}");
		expected.append("}");

		assertThatJson(json).describedAs(expected.toString().replace('\'', '\"'));

		Document readDoc = reader.readValue(json);
		Relationship readRelationship = readDoc.getSingleData().getRelationships().get("project");
		Assert.assertEquals(relationship, readRelationship);
		Assert.assertEquals(doc, readDoc);
	}

	@Test
	public void testMultiValuedRelationship() throws JsonProcessingException, IOException {
		Relationship relationship = new Relationship();
		relationship.setData(Arrays.asList(new ResourceId("3", "projects"), new ResourceId("4", "projects")));
		relationship.setMeta((ObjectNode) objectMapper.readTree("{\"metaName\" : \"metaValue\"}"));
		relationship.setLinks((ObjectNode) objectMapper.readTree("{\"linkName\" : \"linkValue\"}"));

		String json = writer.writeValueAsString(relationship);

		StringBuilder expected = new StringBuilder();
		expected.append("{");
		expected.append("  'project' : [");
		expected.append("     {");
		expected.append("       'data' : [{'id' = '3', 'type' = 'projects'}, {'id' = '4', 'type' = 'projects'}],");
		expected.append("       'meta' : {'metaName' = 'metaValue'},");
		expected.append("       'links' : {'linkName' = 'linkValue'}");
		expected.append("     }");
		expected.append("   ]");
		expected.append("}");

		assertThatJson(json).describedAs(expected.toString().replace('\'', '\"'));

		Relationship readRelationship = objectMapper.reader().forType(Relationship.class).readValue(json);
		Assert.assertEquals(relationship, readRelationship);
	}

	@Test
	public void testMultipleResources() throws JsonProcessingException, IOException {
		Document doc = new Document();

		Resource resource1 = new Resource();
		resource1.setId("1");
		resource1.setType("tasks");
		resource1.setAttribute("name", objectMapper.readTree("\"sample task11\""));

		Resource resource2 = new Resource();
		resource2.setId("2");
		resource2.setType("tasks");
		resource2.setAttribute("name", objectMapper.readTree("\"sample task2\""));

		doc.setData(Arrays.asList(resource1, resource2));

		String json = writer.writeValueAsString(doc);

		StringBuilder expected = new StringBuilder();
		expected.append("{");
		expected.append("\"data\" : [");
		expected.append("  {");
		expected.append("    \"id\" : \"1\",");
		expected.append("    \"type\" : \"tasks\",");
		expected.append("    \"attributes\" : {");
		expected.append("      \"name\" : \"sample task1\"");
		expected.append("      }");
		expected.append("    }");
		expected.append("  }");
		expected.append("  {");
		expected.append("    \"id\" : \"2\",");
		expected.append("    \"type\" : \"tasks\",");
		expected.append("    \"attributes\" : {");
		expected.append("      \"name\" : \"sample task2\"");
		expected.append("      }");
		expected.append("    }");
		expected.append("  }");
		expected.append(" ]");
		expected.append("}");

		assertThatJson(json).describedAs(expected.toString());

		Document readDoc = reader.readValue(json);
		Assert.assertEquals(doc, readDoc);
	}

	@Test
	public void testIncludes() throws JsonProcessingException, IOException {
		Document doc = new Document();

		Resource resource1 = new Resource();
		resource1.setId("1");
		resource1.setType("tasks");
		resource1.setAttribute("name", objectMapper.readTree("\"sample task11\""));

		Resource resource2 = new Resource();
		resource2.setId("2");
		resource2.setType("tasks");
		resource2.setAttribute("name", objectMapper.readTree("\"sample task2\""));

		doc.setIncluded(Arrays.asList(resource1, resource2));

		String json = writer.writeValueAsString(doc);

		StringBuilder expected = new StringBuilder();
		expected.append("{");
		expected.append("\"includes\" : [");
		expected.append("  {");
		expected.append("    \"id\" : \"1\",");
		expected.append("    \"type\" : \"tasks\",");
		expected.append("    \"attributes\" : {");
		expected.append("      \"name\" : \"sample task1\"");
		expected.append("      }");
		expected.append("    }");
		expected.append("  }");
		expected.append("  {");
		expected.append("    \"id\" : \"2\",");
		expected.append("    \"type\" : \"tasks\",");
		expected.append("    \"attributes\" : {");
		expected.append("      \"name\" : \"sample task2\"");
		expected.append("      }");
		expected.append("    }");
		expected.append("  }");
		expected.append(" ]");
		expected.append("}");

		assertThatJson(json).describedAs(expected.toString());

		Document readDoc = reader.readValue(json);
		Assert.assertEquals(doc, readDoc);
	}

}
