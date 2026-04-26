# 3 tags: first 2 root tags (parentId null) + first tag with a non-null parentId.
# Replace IDs, anonymise name and relativeName.
.response.tags |= (
  (.[0:2] + ([.[] | select(.parentId != null)] | .[0:1]))
  | .[0:3]
  | map(
      .id = (.id % 64999 + 1)
      | .name = "Tag \(.id)"
      | .relativeName = "tag \(.id)"
      | .parentId = (if .parentId != null then (.parentId % 64999 + 1) else null end)
    )
)
