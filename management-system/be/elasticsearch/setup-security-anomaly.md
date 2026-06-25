# Security Anomaly Elasticsearch Setup

Run these commands after Elasticsearch/Kibana are available.

```bash
curl -X PUT http://localhost:9200/_ilm/policy/security-anomaly-policy \
  -H 'Content-Type: application/json' \
  --data-binary @elasticsearch/ilm/security-anomaly-policy.json

curl -X PUT http://localhost:9200/_index_template/security-anomaly-template \
  -H 'Content-Type: application/json' \
  --data-binary @elasticsearch/templates/security-anomaly-template.json

curl -X POST http://localhost:5601/api/data_views/data_view \
  -H 'Content-Type: application/json' \
  -H 'kbn-xsrf: true' \
  -d '{"data_view":{"title":"security-anomalies-*","timeFieldName":"@timestamp"}}'
```

Verify:

```bash
curl http://localhost:9200/_cat/indices/security-anomalies-*?v
```
