package synfron.reshaper.burp.core.rules.thens;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import synfron.reshaper.burp.core.messages.EventInfo;
import synfron.reshaper.burp.core.messages.MessageValueHandler;
import synfron.reshaper.burp.core.messages.MessageValueType;
import synfron.reshaper.burp.core.rules.RuleOperationType;
import synfron.reshaper.burp.core.rules.RuleResponse;
import synfron.reshaper.burp.core.utils.TextUtils;
import synfron.reshaper.burp.core.vars.*;

import java.util.Arrays;
import java.util.Collections;

public class ThenSetVariable extends ThenSet<ThenSetVariable> {

    @Getter @Setter
    private VariableSource targetSource = VariableSource.Global;
    @Getter @Setter
    private VariableString variableName;

    public RuleResponse perform(EventInfo eventInfo)
    {
        try {
            String replacementText = getReplacementValue(eventInfo);
            setValue(eventInfo, replacementText);
        } catch (Exception e) {
            if (eventInfo.getDiagnostics().isEnabled()) eventInfo.getDiagnostics().logValue(this, true, Collections.emptyList());
        }
        return RuleResponse.Continue;
    }

    private void setValue(EventInfo eventInfo, String replacementText)
    {
        Variables variables = null;
        switch (targetSource)
        {
            case Event:
                variables = eventInfo.getVariables();
                break;
            case Global:
                variables = GlobalVariables.get();
                break;
        }
        if (variables != null)
        {
            Variable variable = variables.add(variableName.getText(eventInfo));
            if (destinationMessageValuePath != null && destinationMessageValueType != MessageValueType.Text && variable.getValue() != null)
            {
                switch (destinationMessageValueType)
                {
                    case Json:
                        replacementText = TextUtils.setJsonValue(variable.getValue().toString(), destinationMessageValuePath.getText(eventInfo), replacementText);
                        break;
                    case Html:
                        replacementText = TextUtils.setHtmlValue(variable.getValue().toString(), destinationMessageValuePath.getText(eventInfo), replacementText);
                        break;
                }
            }
            variable.setValue(replacementText);
            if (eventInfo.getDiagnostics().isEnabled()) eventInfo.getDiagnostics().logProperties(this, false, Arrays.asList(
                    Pair.of("sourceMessageValue", isUseMessageValue() ? getSourceMessageValue() : null),
                    Pair.of("sourceIdentifier", isUseMessageValue() && MessageValueHandler.hasIdentifier(getSourceMessageValue()) ? VariableString.getTextOrDefault(eventInfo, getSourceIdentifier(), null) : null),
                    Pair.of("sourceValueType", isUseMessageValue() && getSourceMessageValueType() != MessageValueType.Text ? getSourceMessageValueType() : null),
                    Pair.of("sourceValuePath", isUseMessageValue() && getSourceMessageValueType() != MessageValueType.Text ? VariableString.getTextOrDefault(eventInfo, getSourceMessageValuePath(), null) : null),
                    Pair.of("sourceText", !isUseMessageValue() ? VariableString.getTextOrDefault(eventInfo, getText(), null) : null),
                    Pair.of("regexPattern", isUseReplace() ? VariableString.getTextOrDefault(eventInfo, getRegexPattern(), null) : null),
                    Pair.of("replacementText", isUseReplace() ? VariableString.getTextOrDefault(eventInfo, getReplacementText(), null) : null),
                    Pair.of("destinationVariableSource", targetSource),
                    Pair.of("destinationVariableName", VariableString.getTextOrDefault(eventInfo, variableName, null)),
                    Pair.of("destinationValueType", destinationMessageValueType != MessageValueType.Text ? destinationMessageValueType : null),
                    Pair.of("destinationValuePath", destinationMessageValueType != MessageValueType.Text ? VariableString.getTextOrDefault(eventInfo, destinationMessageValuePath, null) : null),
                    Pair.of("input", replacementText)
            ));
        }
    }

    @Override
    public RuleOperationType<ThenSetVariable> getType() {
        return ThenType.SetVariable;
    }
}
