package org.mitre.synthea.engine;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.mitre.synthea.export.JSONSkip;

public class ExpressedSymptom implements Cloneable, Serializable {

  private static final long serialVersionUID = 4322116644425686800L;

  // this class contains basic info regarding an expressed symptoms.
  // such as the cause and the associated value
  public class SymptomInfo implements Cloneable, Serializable {
    private static final long serialVersionUID = 4322116644425686801L;
    // what is the value associated to that symptom
    private Integer value;
    // At which time the symptom was set
    private Long time;

    /**
     * Create a new instance for the supplied cause, value and time.
     */
    public SymptomInfo(Integer value, Long time) {
      this.value = value;
      this.time = time;
    }

    public SymptomInfo clone() {
      return new SymptomInfo(this.value, this.time);
    }

    public Integer getValue() {
      return value;
    }

    public Long getTime() {
      return time;
    }
  }

  // this class encapsulates module-based infos regarding an expressed symptoms.
  public class SymptomSource implements Cloneable, Serializable {
    private static final long serialVersionUID = 4322116644425686802L;

    @JSONSkip
    ExpressedSymptom symptom = ExpressedSymptom.this;
    // From which module the expressed symptom was set
    private String source;
    // what is the status from a given expressed symptom from a given module
    private boolean resolved;
    // the time on which the expressed symptom was updated and the associated info.
    private LinkedList<SymptomInfo> symptomInfos;

    /**
     * Create a new instance for the supplied module source.
     */
    public SymptomSource(String source) {
      this.source = source;
      symptomInfos = new LinkedList<>();
      resolved = false;
    }

    /**
     * Create shallow copy of this instance.
     */
    public SymptomSource clone() {
      SymptomSource data = new SymptomSource(this.source);
      data.resolved = this.resolved;
      data.symptomInfos.addAll(this.symptomInfos);
      return data;
    }

    public boolean isResolved() {
      return resolved;
    }

    public void resolve() {
      this.resolved = true;
    }

    public void activate() {
      this.resolved = false;
    }

    public Long getLastUpdateTime() {
      if (this.symptomInfos.isEmpty()) {
        return null;
      }
      return this.symptomInfos.getLast().getTime();
    }

    public String getSource() {
      return source;
    }

    /**
     * Record a new symptom.
     */
    public void addInfo(long time, int value, Boolean addressed) {
      SymptomInfo info = new SymptomInfo(value, time);
      symptomInfos.add(info);
      resolved = addressed;
    }

    /**
     * Get the current value of the symptom.
     */
    public Integer getCurrentValue() {
      if (this.symptomInfos.isEmpty()) {
        return null;
      }
      return this.symptomInfos.getLast().getValue();
    }

    public Integer getValueAtTime(long time) {
      Optional<SymptomInfo> symptomInfo = this.symptomInfos.stream()
              .filter(si -> si.getTime() == time).findFirst();
      if (symptomInfo.isPresent()) {
        return symptomInfo.get().getValue();
      } else {
        return null;
      }
    }

    /**
     * Get the times for this symptom.
     */
    public List<SymptomInfo> getSymptomInfos() {
      return symptomInfos;
    }
  }

  //keep track of the different sources of the expressed conditions
  private Map<String, SymptomSource> sources;
  private String name;

  public ExpressedSymptom(String name) {
    this.name = name;
    sources = new ConcurrentHashMap<String, SymptomSource>();
  }

  /**
   * Create a shallow copy of this instance.
   */
  public ExpressedSymptom clone() {
    ExpressedSymptom data = new ExpressedSymptom(this.name);
    data.sources.putAll(this.sources);
    return data;
  }

  public Map<String, SymptomSource> getSources() {
    return sources;
  }

  /** this method updates the data structure wit a symptom being onset from a module.
   */
  public void onSet(String module, long time, int value, Boolean addressed) {
    if (!sources.containsKey(module)) {
      sources.put(module, new SymptomSource(module));
    }
    sources.get(module).addInfo(time, value, addressed);
  }

  /**
   * Method for retrieving the value associated to a given symptom.
   * This correspond to the maximum value across all potential causes.
   */
  public int getSymptom() {
    int max = 0;
    for (String module : sources.keySet()) {
      Integer value = sources.get(module).getCurrentValue();
      Boolean isResolved = sources.get(module).isResolved();
      if (value != null && value.intValue() > max && !isResolved) {
        max = value.intValue();
      }
    }
    return max;
  }

  /**
   * Method for retrieving the source with the high value not yet addressed.
   */
  public String getSourceWithHighValue() {
    String result = null;
    int max = 0;
    for (String module : sources.keySet()) {
      Boolean isResolved = sources.get(module).isResolved();
      Integer value = sources.get(module).getCurrentValue();
      if (result == null && value != null && !isResolved) {
        result = module;
        max = value.intValue();
      } else if (value != null && value.intValue() > max && !isResolved) {
        result = module;
        max = value.intValue();
      }
    }
    return result;
  }

  /**
   * Method for retrieving the value associated to a given source.
   */
  public Integer getValueFromSource(String source) {
    if (source == null || !sources.containsKey(source)) {
      return null;
    }
    return sources.get(source).getCurrentValue();
  }

  /**
   * Method for addressing a given source.
   */
  public void addressSource(String source) {
    if (source != null && sources.containsKey(source)) {
      sources.get(source).resolve();
    }
  }

  /**
   * Method for retrieving the last time the symptom has been updated from a given module.
   */
  public Long getSymptomLastUpdatedTime(String module) {
    Long result = null;
    if (module != null && sources.containsKey(module)) {
      result = sources.get(module).getLastUpdateTime();
    }
    return result;
  }
}
