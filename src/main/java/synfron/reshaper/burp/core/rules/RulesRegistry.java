package synfron.reshaper.burp.core.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import synfron.reshaper.burp.core.events.*;
import synfron.reshaper.burp.core.settings.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RulesRegistry {
    @Getter
    private transient int version;

    private final IEventListener<PropertyChangedArgs> rulePropertyChangedListener = this::onRulePropertyChanged;

    private List<Rule> rules = new ArrayList<>();

    public List<Rule> getRules() {
        return new ArrayList<>(rules);
    }

    @Getter
    private final CollectionChangedEvent collectionChangedEvent = new CollectionChangedEvent();

    public synchronized void deleteRule(Rule rule) {
        int index = rules.indexOf(rule);
        if (index >= 0) {
            rules.remove(index);
            version++;
            collectionChangedEvent.invoke(new CollectionChangedArgs(this, CollectionChangedAction.Remove, index, rule));
        }
    }

    public synchronized void addRule(Rule rule) {
        rules.add(rule);
        version++;
        collectionChangedEvent.invoke(new CollectionChangedArgs(this, CollectionChangedAction.Add, rules.size() - 1, rule));
        rule.getPropertyChangedEvent().add(rulePropertyChangedListener);
    }

    private void onRulePropertyChanged(PropertyChangedArgs propertyChangedArgs) {
        Rule rule = (Rule)propertyChangedArgs.getSource();
        int index = rules.indexOf(rule);
        if (index >= 0) {
            version++;
            collectionChangedEvent.invoke(new CollectionChangedArgs(this, CollectionChangedAction.Update, index, rule));
        }
    }

    public synchronized void movePrevious(Rule rule)
    {
        if (rule != null) {
            int currentIndex = rules.indexOf(rule);
            if (currentIndex > 0) {

                rules.remove(currentIndex);
                rules.add(--currentIndex, rule);
                version++;
                collectionChangedEvent.invoke(new CollectionChangedArgs(this, CollectionChangedAction.Move, currentIndex + 1, currentIndex, rule));
            }
        }
    }

    public synchronized void moveNext(Rule rule)
    {
        if (rule != null)
        {
            int currentIndex = rules.indexOf(rule);
            if (currentIndex < rules.size() - 1)
            {
                rules.remove(currentIndex);
                rules.add(++currentIndex, rule);
                version++;
                collectionChangedEvent.invoke(new CollectionChangedArgs(this, CollectionChangedAction.Move, currentIndex - 1, currentIndex, rule));
            }
        }
    }

    public void loadRules() {
        rules = ObjectUtils.defaultIfNull(Settings.get("Reshaper.rules", new TypeReference<>() {}), rules);
        rules.forEach(rule -> rule.getPropertyChangedEvent().add(rulePropertyChangedListener));
        version++;
    }

    public void saveRules() {
        Settings.store("Reshaper.rules", rules.stream().filter(rule -> StringUtils.isNotEmpty(rule.getName())).collect(Collectors.toList()));
    }
}
