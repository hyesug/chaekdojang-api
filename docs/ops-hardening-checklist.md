# Ops Hardening Checklist

This checklist covers the production/staging work that cannot be fully completed from code alone.

## S3 DB Backup Storage

Code is ready:

- `scripts/backup-prod-db.sh` creates a PostgreSQL custom dump.
- If `BACKUP_S3_BUCKET` is set, the dump is also copied to S3.
- `scripts/install-db-backup-cron.sh` installs a daily cron job.

AWS work:

- Created private S3 bucket: `chaekdojang-prod-backups-863518416212`.
- Turn on bucket versioning.
- Turn on default server-side encryption with SSE-S3 or SSE-KMS.
- Block all public access.
- Add a lifecycle rule:
  - transition old backups to cheaper storage if needed,
  - expire backups after the retention period you choose.
- Attach an EC2 instance role that can write only to the backup prefix.

EC2 `.env.production` example:

```text
BACKUP_S3_BUCKET=chaekdojang-prod-backups-863518416212
BACKUP_S3_PREFIX=db-backups/prod
RETENTION_DAYS=14
```

Current EC2 status:

- Daily backup cron installed at `03:17 KST`.
- A test backup was uploaded to S3 successfully.

## AWS Cost Alert

Code is ready:

```bash
cd ~/chaekdojang-api
MONTHLY_LIMIT_USD=20 ./scripts/create-monthly-budget-alert.sh your-email@example.com
```

AWS work:

- Confirm the AWS Budgets email subscription from your inbox.
- Adjust `MONTHLY_LIMIT_USD` to your real comfort limit.
- Recommended starter alerts:
  - actual cost over 80 percent,
  - forecasted cost over 100 percent.

Current AWS status:

- Monthly budget `chaekdojang-monthly-cost` was created at `20 USD`.
- Email subscription confirmation is still required in the inbox.

## Security Group Final Check

Code is ready:

```bash
cd ~/chaekdojang-api
./scripts/audit-security-group.sh sg-xxxxxxxx
```

Expected inbound rules:

- `80/tcp`: public, for HTTP redirect and certificate renewal.
- `443/tcp`: public, for HTTPS.
- `22/tcp`: your current IP only.
- `8080/tcp`: not public.
- `5432/tcp`: not public.
- `6379/tcp`: not public.

Current AWS status:

- EC2 security group allows public `80/tcp` and `443/tcp` only.
- SSH `22/tcp` is restricted to the current admin IP.
- RDS is `PubliclyAccessible=false`.

## DB Index Check

Code is ready:

- `V2__admin_audit_logs_and_operational_indexes.sql` creates `admin_audit_logs` and its indexes through Flyway.
- `scripts/apply-operational-indexes.sql` contains indexes for existing production tables. Run it with the owner of those tables, because the application DB user may not own legacy tables.

Deploy check:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

After real traffic, inspect slow queries:

```sql
SELECT query, calls, mean_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

`pg_stat_statements` must be enabled on RDS before using the second query.

## Staging Environment

Code is ready:

- Backend: `docker-compose.staging.yml`
- Backend env template: `.env.staging.example`
- Frontend env template: `../chaekdojang-web/docs/staging-env.example`

AWS/Vercel work:

- Create a separate staging database or at least a separate database name.
- Create `staging-api.chaekdojang.com` and route it to EC2/Nginx.
- Add an Nginx server block that proxies staging API traffic to `127.0.0.1:8081`.
- Create a Vercel staging project or preview environment.
- Set frontend env:
  - `BACKEND_URL=https://staging-api.chaekdojang.com`
  - `NEXT_PUBLIC_API_BASE_URL=https://staging-api.chaekdojang.com`
- Set backend env:
  - `FRONTEND_URL=https://staging.chaekdojang.com`
  - `BACKEND_URL=https://staging-api.chaekdojang.com`
  - `CORS_ALLOWED_ORIGINS=<Vercel staging URL>`

## Admin Audit Log

Code is ready:

- New table: `admin_audit_logs`
- New API: `GET /api/admin/audit-logs`
- Recorded actions:
  - user role changes,
  - review hidden/unhidden changes,
  - admin inquiry comments.

Frontend work still recommended:

- Add an "Audit Logs" tab in the admin page.
- Show actor, action, target, summary, and time.

## S3 File Upload

Code is ready:

- Existing multipart endpoint still works: `POST /api/upload/profile-image`.
- When `STORAGE_TYPE=s3`, the same endpoint uploads to S3.
- Direct upload preparation endpoint is ready: `POST /api/upload/profile-image/presigned-url`.

Backend env example:

```text
STORAGE_TYPE=s3
S3_UPLOAD_BUCKET=chaekdojang-prod-uploads-863518416212
AWS_REGION=ap-northeast-2
S3_PUBLIC_BASE_URL=https://cdn.chaekdojang.com
S3_PROFILE_IMAGE_PREFIX=profile-images
S3_PRESIGNED_URL_EXPIRATION_MINUTES=10
```

AWS work:

- Created private upload bucket: `chaekdojang-prod-uploads-863518416212`.
- Attach an EC2 role with `s3:PutObject` for `profile-images/*`.
- Decide how files become publicly readable:
  - recommended: CloudFront in front of the bucket,
  - simpler but less controlled: public-read object policy.
- Add bucket CORS if using direct browser uploads:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT"],
    "AllowedOrigins": ["https://www.chaekdojang.com", "https://staging.chaekdojang.com"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```
