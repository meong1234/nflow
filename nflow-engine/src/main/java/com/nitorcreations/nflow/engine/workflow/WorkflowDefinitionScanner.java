package com.nitorcreations.nflow.engine.workflow;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import com.nitorcreations.nflow.engine.workflow.WorkflowStateMethod.StateParameter;

public class WorkflowDefinitionScanner {
  private static final Set<Type> knownImmutableTypes = new HashSet<>();
  {
    knownImmutableTypes.addAll(asList(Integer.TYPE, Integer.class, String.class));
  }

  public Map<String, WorkflowStateMethod> getStateMethods(@SuppressWarnings("rawtypes") Class<? extends WorkflowDefinition> definition) {
    final Map<String, WorkflowStateMethod> methods = new HashMap<>();
    ReflectionUtils.doWithMethods(definition, new MethodCallback() {
      @Override
      public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
        List<StateParameter> params = new ArrayList<>();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 1; i < genericParameterTypes.length; ++i) {
          for (Annotation a : parameterAnnotations[i]) {
            if (Data.class.equals(a.annotationType())) {
              Type type = genericParameterTypes[i];
              Data data = (Data) a;
              params.add(new StateParameter(data.value(), type, defaultValue(type), data.readOnly() || isReadOnly(type)));
              break;
            }
          }
        }
        if (params.size() != genericParameterTypes.length - 1) {
          throw new IllegalStateException("Not all parameter names could be resolved for " + method + ". Maybe missing @Data annotation?");
        }
        methods.put(method.getName(), new WorkflowStateMethod(method, params.toArray(new StateParameter[params.size()])));
      }
    }, new WorkflowTransitionMethod());
    return methods;
  }

  boolean isReadOnly(Type type) {
    return knownImmutableTypes.contains(type);
  }

  Object defaultValue(Type type) {
    Class<?> clazz = (Class<?>) type;
    if (clazz.isPrimitive()) {
      if (Boolean.TYPE.equals(clazz)) {
        return Boolean.FALSE;
      }
      return ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(clazz, "valueOf", String.class), null, "0");
    }
    return null;
  }

  static final class WorkflowTransitionMethod implements MethodFilter {
    @Override
    public boolean matches(Method method) {
      int mod = method.getModifiers();
      Class<?>[] parameterTypes = method.getParameterTypes();
      return isPublic(mod) && !isStatic(mod) && parameterTypes.length >= 1 && StateExecution.class.equals(parameterTypes[0]);
    }
  }
}