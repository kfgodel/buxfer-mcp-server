# Reminder names can contain personal context (e.g. "Rent to John").
# Replace names while keeping amounts, dates, and account references.
.reminders |= map(
  .name = "Reminder \(.id)"
)
