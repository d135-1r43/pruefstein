package com.pruefstein.report.flow;

import java.util.List;

import io.serverlessworkflow.impl.marshaller.CustomObjectMarshaller;
import io.serverlessworkflow.impl.marshaller.DefaultBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import io.serverlessworkflow.impl.model.jackson.JacksonModel;
import io.serverlessworkflow.impl.model.jackson.JacksonModelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Overrides the {@code @DefaultBean} {@link WorkflowBufferFactory} from
 * {@code quarkus-flow-persistence-common} to register a
 * {@link CustomObjectMarshaller} for {@link JacksonModel}.
 *
 * <p>
 * Without this, {@code WorkflowModelConverter} (from quarkus-flow-jpa) fails to
 * persist the workflow state because {@code AbstractOutputBuffer} has no
 * built-in handler for {@code JacksonModel}.
 */
@ApplicationScoped
class WorkflowBufferFactoryProducer
{
	@Produces
	@ApplicationScoped
	WorkflowBufferFactory workflowBufferFactory()
	{
		return new DefaultBufferFactory(List.of(new JacksonMarshaller()))
		{
		};
	}

	static class JacksonMarshaller implements CustomObjectMarshaller<JacksonModel>
	{
		private static final JacksonModelFactory FACTORY = new JacksonModelFactory();

		@Override
		public Class<JacksonModel> getObjectClass()
		{
			return JacksonModel.class;
		}

		@Override
		public void write(WorkflowOutputBuffer out, JacksonModel model)
		{
			out.writeObject(model.asJavaObject());
		}

		@Override
		public JacksonModel read(WorkflowInputBuffer in)
		{
			return (JacksonModel)FACTORY.fromOther(in.readObject());
		}
	}
}
