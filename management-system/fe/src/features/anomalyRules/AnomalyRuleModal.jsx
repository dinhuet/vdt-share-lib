import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import { formToPayload, OPERATORS, RULE_SEVERITIES, RULE_TYPES, ruleToForm, SCOPE_TYPES, TIME_BUCKET_TYPES } from './anomalyRules.helpers';

export default function AnomalyRuleModal({ rule, scopeOptions = {}, saving, onClose, onSave }) {
  const [form, setForm] = useState(() => ruleToForm(rule));
  const [error, setError] = useState('');

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function updateScopeType(value) {
    setForm((current) => ({ ...current, scopeType: value, scopeId: '' }));
  }

  function submit() {
    const payload = formToPayload(form);
    if (!payload.ruleCode || !payload.name || !payload.metric) {
      setError('Rule code, name, and metric are required.');
      return;
    }
    if (payload.scopeType !== 'GLOBAL' && !payload.scopeId) {
      setError('Non-global rules require a scope id.');
      return;
    }
    setError('');
    onSave(payload);
  }

  const showStatic = form.ruleType === 'STATIC' || form.ruleType === 'HYBRID';
  const showBaseline = form.ruleType === 'BASELINE' || form.ruleType === 'HYBRID';
  const currentScopeOptions = scopeOptions[form.scopeType] || [];
  const hasScopeOptions = currentScopeOptions.length > 0;
  const selectedScopeIdInOptions = currentScopeOptions.some((option) => option.value === form.scopeId);

  return (
    <Modal
      title={rule ? 'Edit Anomaly Rule' : 'Create Anomaly Rule'}
      description="Manage anomaly rule severity, cooldown, and detection thresholds."
      onClose={onClose}
      footer={(<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button disabled={saving} onClick={submit}>{saving ? 'Saving...' : 'Save Rule'}</Button></>)}
    >
      <div className="form-grid wide">
        <label><span>Rule Code</span><input value={form.ruleCode} onChange={(event) => updateField('ruleCode', event.target.value)} /></label>
        <label><span>Name</span><input value={form.name} onChange={(event) => updateField('name', event.target.value)} /></label>
        <label><span>Metric</span><input value={form.metric} onChange={(event) => updateField('metric', event.target.value)} /></label>
        <label><span>Rule Type</span><select value={form.ruleType} onChange={(event) => updateField('ruleType', event.target.value)}>{RULE_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}</select></label>
        <label><span>Severity</span><select value={form.severity} onChange={(event) => updateField('severity', event.target.value)}>{RULE_SEVERITIES.map((severity) => <option key={severity} value={severity}>{severity}</option>)}</select></label>
        <label><span>Scope Type</span><select value={form.scopeType} onChange={(event) => updateScopeType(event.target.value)}>{SCOPE_TYPES.map((scope) => <option key={scope} value={scope}>{scope}</option>)}</select></label>
        {form.scopeType !== 'GLOBAL' ? <label><span>Scope ID</span>{hasScopeOptions ? (
          <select value={form.scopeId} onChange={(event) => updateField('scopeId', event.target.value)}>
            <option value="">Select {form.scopeType.toLowerCase().replaceAll('_', ' ')}</option>
            {form.scopeId && !selectedScopeIdInOptions ? <option value={form.scopeId}>{form.scopeId}</option> : null}
            {currentScopeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        ) : <input value={form.scopeId} onChange={(event) => updateField('scopeId', event.target.value)} placeholder={form.scopeType === 'ENDPOINT_IP' ? 'IP address' : 'Scope id'} />}</label> : null}
        <label><span>Cooldown Minutes</span><input type="number" min="0" value={form.cooldownMinutes} onChange={(event) => updateField('cooldownMinutes', event.target.value)} /></label>
        <label className="checkbox-label"><input type="checkbox" checked={form.enabled} onChange={(event) => updateField('enabled', event.target.checked)} />Enabled</label>
        <label className="wide-field"><span>Description</span><textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} /></label>
      </div>
      {showStatic ? (
        <><h3 className="modal-section-title">Static config</h3><div className="form-grid wide">
          <label><span>Operator</span><select value={form.operator} onChange={(event) => updateField('operator', event.target.value)}>{OPERATORS.map((operator) => <option key={operator} value={operator}>{operator}</option>)}</select></label>
          <label><span>Threshold Value</span><input type="number" min="0" value={form.thresholdValue} onChange={(event) => updateField('thresholdValue', event.target.value)} /></label>
          <label><span>Window Seconds</span><input type="number" min="1" value={form.staticWindowSeconds} onChange={(event) => updateField('staticWindowSeconds', event.target.value)} /></label>
          <label><span>Min Count</span><input type="number" min="1" value={form.minCount} onChange={(event) => updateField('minCount', event.target.value)} /></label>
        </div></>
      ) : null}
      {showBaseline ? (
        <><h3 className="modal-section-title">Baseline config</h3><div className="form-grid wide">
          <label><span>History Days</span><input type="number" min="1" value={form.historyDays} onChange={(event) => updateField('historyDays', event.target.value)} /></label>
          <label><span>Time Bucket Type</span><select value={form.timeBucketType} onChange={(event) => updateField('timeBucketType', event.target.value)}>{TIME_BUCKET_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}</select></label>
          <label><span>Percentile</span><input type="number" min="1" max="100" value={form.percentile} onChange={(event) => updateField('percentile', event.target.value)} /></label>
          <label><span>Multiplier</span><input type="number" min="0" step="0.01" value={form.multiplier} onChange={(event) => updateField('multiplier', event.target.value)} /></label>
          <label><span>Min Absolute Threshold</span><input type="number" min="0" value={form.minAbsoluteThreshold} onChange={(event) => updateField('minAbsoluteThreshold', event.target.value)} /></label>
          <label><span>Window Seconds</span><input type="number" min="1" value={form.baselineWindowSeconds} onChange={(event) => updateField('baselineWindowSeconds', event.target.value)} /></label>
        </div></>
      ) : null}
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
