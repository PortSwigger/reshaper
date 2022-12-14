package synfron.reshaper.burp.core.rules.thens.entities.script;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import synfron.reshaper.burp.core.ProtocolType;
import synfron.reshaper.burp.core.messages.EventInfo;
import synfron.reshaper.burp.core.messages.MessageValue;
import synfron.reshaper.burp.core.messages.MessageValueHandler;
import synfron.reshaper.burp.core.messages.WebSocketEventInfo;
import synfron.reshaper.burp.core.rules.RuleOperationType;
import synfron.reshaper.burp.core.rules.RuleResponse;
import synfron.reshaper.burp.core.rules.thens.Then;
import synfron.reshaper.burp.core.rules.thens.ThenType;
import synfron.reshaper.burp.core.utils.GetItemPlacement;
import synfron.reshaper.burp.core.utils.Serializer;
import synfron.reshaper.burp.core.utils.SetItemPlacement;
import synfron.reshaper.burp.core.utils.TextUtils;
import synfron.reshaper.burp.core.vars.GlobalVariables;
import synfron.reshaper.burp.core.vars.Variable;
import synfron.reshaper.burp.core.vars.VariableString;
import synfron.reshaper.burp.core.vars.Variables;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReshaperObj {
    public VariablesObj variables = new VariablesObj();
    public EventObj event = new EventObj();

    public static class VariablesObj {

        public String getGlobalVariable(String name) {
            return getVariable(GlobalVariables.get(), name);
        }

        public String getEventVariable(String name) {
            return getVariable(((EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo")).getVariables(), name);
        }

        public String getSessionVariable(String name) {
            EventInfo eventInfo = (EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo");
            if (eventInfo instanceof WebSocketEventInfo<?>) {
                WebSocketEventInfo<?> webSocketEventInfo = (WebSocketEventInfo<?>) eventInfo;
                return getVariable(webSocketEventInfo.getSessionVariables(), name);
            }
            return null;
        }

        private String getVariable(Variables variables, String name) {
            Variable variable = variables.getOrDefault(name);
            return variable != null ?
                    TextUtils.toString(variable.getValue()) :
                    null;
        }

        public void setGlobalVariable(String name, String value) {
            if (!VariableString.isValidVariableName(name)) {
                throw new IllegalArgumentException(String.format("Invalid variable name '%s'", name));
            }
            GlobalVariables.get().add(name).setValue(value);
        }

        public void setEventVariable(String name, String value) {
            if (!VariableString.isValidVariableName(name)) {
                throw new IllegalArgumentException(String.format("Invalid variable name '%s'", name));
            }
            ((EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo")).getVariables().add(name).setValue(value);
        }

        public void setSessionVariable(String name, String value) {
            if (!VariableString.isValidVariableName(name)) {
                throw new IllegalArgumentException(String.format("Invalid variable name '%s'", name));
            }
            EventInfo eventInfo = (EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo");
            if (eventInfo instanceof WebSocketEventInfo<?>) {
                WebSocketEventInfo<?> webSocketEventInfo = (WebSocketEventInfo<?>) eventInfo;
                webSocketEventInfo.getSessionVariables().add(name).setValue(value);
            }
        }

        public void deleteGlobalVariable(String name) {
            GlobalVariables.get().remove(name);
        }

        public void deleteEventVariable(String name) {
            ((EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo")).getVariables().remove(name);
        }

        public void deleteSessionVariable(String name) {
            EventInfo eventInfo = (EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo");
            if (eventInfo instanceof WebSocketEventInfo<?>) {
                WebSocketEventInfo<?> webSocketEventInfo = (WebSocketEventInfo<?>) eventInfo;
                webSocketEventInfo.getSessionVariables().remove(name);
            }
        }
    }

    public static class EventObj {

        public List<String> getMessageValueKeys() {
            EventInfo eventInfo = (EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo");
            return Arrays.stream(MessageValue.values())
                    .filter(value -> value.hasProtocolType(eventInfo.getProtocolType()))
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }

        public String getMessageValue(String key, String identifier) {
            MessageValue messageValue = EnumUtils.getEnumIgnoreCase(MessageValue.class, key);
            EventInfo eventInfo = (EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo");
            if (messageValue == null || !messageValue.hasProtocolType(eventInfo.getProtocolType())) {
                throw new IllegalArgumentException(String.format("Invalid message value key: '%s'", key));
            }
            return MessageValueHandler.getValue(
                    eventInfo,
                    EnumUtils.getEnumIgnoreCase(MessageValue.class, key),
                    VariableString.getAsVariableString(identifier, false),
                    GetItemPlacement.Last
            );
        }

        public void setMessageValue(String key, String identifier, String value) {
            MessageValue messageValue = EnumUtils.getEnumIgnoreCase(MessageValue.class, key);
            EventInfo eventInfo = (EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo");
            if (messageValue == null || !messageValue.hasProtocolType(eventInfo.getProtocolType())) {
                throw new IllegalArgumentException(String.format("Invalid message value key: '%s'", key));
            }
            MessageValueHandler.setValue(
                    eventInfo,
                    EnumUtils.getEnumIgnoreCase(MessageValue.class, key),
                    VariableString.getAsVariableString(identifier, false),
                    SetItemPlacement.Only,
                    value
            );
        }

        public String runThen(String thenType, NativeObject thenData) {
            Dispatcher dispatcher = Dispatcher.getCurrent();
            EventInfo eventInfo = (EventInfo)Dispatcher.getCurrent().getDataBag().get("eventInfo");
            String adjustedThenTypeName = StringUtils.prependIfMissing(thenType, "Then");
            Stream<ThenType<?>> supportedThenTypes = Stream.of(
                    ThenType.Highlight,
                    ThenType.Comment,
                    ThenType.Evaluate,
                    ThenType.BuildHttpMessage,
                    ThenType.DeleteValue,
                    ThenType.DeleteVariable,
                    ThenType.Drop,
                    ThenType.Intercept,
                    ThenType.Log,
                    ThenType.ParseHttpMessage,
                    ThenType.SendRequest,
                    ThenType.SendMessage,
                    ThenType.SendTo,
                    ThenType.SetEventDirection,
                    ThenType.SetEncoding,
                    ThenType.SetValue,
                    ThenType.SetVariable
            );
            Class<?> thenClass = supportedThenTypes
                    .filter(type -> eventInfo.getProtocolType().accepts(ProtocolType.fromRuleOperationType(type.getType())))
                    .filter(type -> type.getType().getSimpleName().equalsIgnoreCase(adjustedThenTypeName))
                    .map(RuleOperationType::getType)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Then type '%s' is not available", thenType)));
            thenData.defineProperty("@class", "." + thenClass.getSimpleName(), 0);
            String thenDataJson = NativeJSON.stringify(
                    dispatcher.getContext(),
                    dispatcher.getContext().initSafeStandardObjects(),
                    thenData,
                    null,
                    null
            ).toString();
            Then<?> then = (Then<?>)Serializer.deserialize(thenDataJson, thenClass);
            return then.perform(eventInfo).toString();
        }

        public void setRuleResponse(String ruleResponse) {
            switch (ruleResponse.toUpperCase()) {
                case "CONTINUE":
                    Dispatcher.getCurrent().getDataBag().put("ruleResponse", RuleResponse.Continue);
                    break;
                case "BREAKTHENS":
                    Dispatcher.getCurrent().getDataBag().put("ruleResponse", RuleResponse.BreakThens);
                    break;
                case "BREAKRULES":
                    Dispatcher.getCurrent().getDataBag().put("ruleResponse", RuleResponse.BreakRules);
                    break;
            }
        }
    }
}
