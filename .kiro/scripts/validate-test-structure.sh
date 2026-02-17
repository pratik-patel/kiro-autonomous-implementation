#!/bin/bash

# Test Structure Validator for Spring Boot + JUnit 5
# Enforces: AAA/GWT markers, spec traceability, assertions, anti-patterns
# Usage: ./validate-test-structure.sh [optional-file-path]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TEST_DIR="$PROJECT_ROOT/src/test/java"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
VIOLATIONS=0
CHECKED_FILES=0
CHECKED_TESTS=0

# If a specific file is provided, validate only that file
if [ -n "$1" ]; then
  TARGET_FILE="$1"
  if [ ! -f "$TARGET_FILE" ]; then
    echo -e "${RED}Error: File not found: $TARGET_FILE${NC}"
    exit 1
  fi
  FILES_TO_CHECK=("$TARGET_FILE")
else
  # Find all test files
  if [ ! -d "$TEST_DIR" ]; then
    echo -e "${YELLOW}No test directory found at $TEST_DIR${NC}"
    exit 0
  fi
  mapfile -t FILES_TO_CHECK < <(find "$TEST_DIR" -name "*.java" -type f)
fi

if [ ${#FILES_TO_CHECK[@]} -eq 0 ]; then
  echo -e "${GREEN}No test files to validate${NC}"
  exit 0
fi

# Temporary file for parsing
TEMP_FILE=$(mktemp)
trap "rm -f $TEMP_FILE" EXIT

# Function to extract test methods from a Java file
# Returns: method_name|line_number
extract_test_methods() {
  local file="$1"
  grep -n "@Test" "$file" | while read -r line; do
    line_num=$(echo "$line" | cut -d: -f1)
    # Look for the method declaration on the next few lines
    sed -n "${line_num},$((line_num+5))p" "$file" | grep -E "^\s*(public|private|protected)?\s+void\s+\w+\(" | head -1 | sed 's/.*\s\(\w\+\)(.*/\1/' | while read -r method; do
      if [ -n "$method" ]; then
        echo "$method|$line_num"
      fi
    done
  done
}

# Function to check if test has AAA or GWT markers
has_aaa_or_gwt_markers() {
  local file="$1"
  local start_line="$2"
  local end_line="$3"
  
  sed -n "${start_line},${end_line}p" "$file" | grep -qE "//\s*(Arrange|Act|Assert|Given|When|Then)" && return 0
  return 1
}

# Function to check for spec traceability
has_spec_traceability() {
  local file="$1"
  local start_line="$2"
  local end_line="$3"
  
  # Check for @Tag with spec or ac
  sed -n "${start_line},${end_line}p" "$file" | grep -qE '@Tag\("(spec:|ac:)' && return 0
  
  # Check for @DisplayName with [SPEC: or [AC:
  sed -n "${start_line},${end_line}p" "$file" | grep -qE '@DisplayName.*\[(SPEC|AC):' && return 0
  
  # Check for comment with SPEC: or AC:
  sed -n "${start_line},${end_line}p" "$file" | grep -qE '//\s*(SPEC|AC):' && return 0
  
  return 1
}

# Function to check for assertions
has_assertions() {
  local file="$1"
  local start_line="$2"
  local end_line="$3"
  
  sed -n "${start_line},${end_line}p" "$file" | grep -qE '(Assertions\.assert|assertThat\(|assertEquals|assertTrue|assertFalse|assertNull|assertNotNull|assertThrows)' && return 0
  return 1
}

# Function to check for anti-patterns
check_anti_patterns() {
  local file="$1"
  local start_line="$2"
  local end_line="$3"
  local method_name="$4"
  
  local violations=""
  
  # Check for Thread.sleep()
  if sed -n "${start_line},${end_line}p" "$file" | grep -q "Thread\.sleep"; then
    violations="${violations}    - Thread.sleep() detected (use @Timeout or testcontainers instead)\n"
  fi
  
  # Check for System.out/err
  if sed -n "${start_line},${end_line}p" "$file" | grep -qE "System\.(out|err)\.print"; then
    violations="${violations}    - System.out/err print detected (use logging instead)\n"
  fi
  
  # Check for @Disabled without ticket reference
  if sed -n "${start_line},${end_line}p" "$file" | grep -q "@Disabled"; then
    if ! sed -n "${start_line},${end_line}p" "$file" | grep -qE '(@Tag\("ticket:|// TICKET:)'; then
      violations="${violations}    - @Disabled without ticket reference (add @Tag(\"ticket:XYZ\") or // TICKET: XYZ)\n"
    fi
  fi
  
  echo -e "$violations"
}

# Function to find the end of a method
find_method_end() {
  local file="$1"
  local start_line="$2"
  
  local brace_count=0
  local found_opening=0
  local line_num=$start_line
  local total_lines=$(wc -l < "$file")
  
  while [ $line_num -le $total_lines ]; do
    local line=$(sed -n "${line_num}p" "$file")
    
    # Count braces
    brace_count=$((brace_count + $(echo "$line" | grep -o '{' | wc -l)))
    brace_count=$((brace_count - $(echo "$line" | grep -o '}' | wc -l)))
    
    if [ $brace_count -gt 0 ]; then
      found_opening=1
    fi
    
    if [ $found_opening -eq 1 ] && [ $brace_count -eq 0 ]; then
      echo $line_num
      return 0
    fi
    
    line_num=$((line_num + 1))
  done
  
  echo $total_lines
}

# Main validation loop
for file in "${FILES_TO_CHECK[@]}"; do
  CHECKED_FILES=$((CHECKED_FILES + 1))
  
  # Extract test methods
  while IFS='|' read -r method_name line_num; do
    if [ -z "$method_name" ]; then
      continue
    fi
    
    CHECKED_TESTS=$((CHECKED_TESTS + 1))
    method_end=$(find_method_end "$file" "$line_num")
    
    # Check AAA/GWT markers
    if ! has_aaa_or_gwt_markers "$file" "$line_num" "$method_end"; then
      echo -e "${RED}✗ VIOLATION: $file${NC}"
      echo -e "  Method: ${YELLOW}$method_name${NC} (line $line_num)"
      echo -e "  Rule: ${RED}Missing AAA/GWT markers${NC}"
      echo -e "  Fix: Add explicit // Arrange, // Act, // Assert or // Given, // When, // Then comments"
      VIOLATIONS=$((VIOLATIONS + 1))
    fi
    
    # Check spec traceability
    if ! has_spec_traceability "$file" "$line_num" "$method_end"; then
      echo -e "${RED}✗ VIOLATION: $file${NC}"
      echo -e "  Method: ${YELLOW}$method_name${NC} (line $line_num)"
      echo -e "  Rule: ${RED}Missing spec traceability${NC}"
      echo -e "  Fix: Add @Tag(\"spec:SPEC_ID\") or @Tag(\"ac:REQ-1.2\") or @DisplayName(\"[SPEC: id]\") or // SPEC: id comment"
      VIOLATIONS=$((VIOLATIONS + 1))
    fi
    
    # Check assertions
    if ! has_assertions "$file" "$line_num" "$method_end"; then
      echo -e "${RED}✗ VIOLATION: $file${NC}"
      echo -e "  Method: ${YELLOW}$method_name${NC} (line $line_num)"
      echo -e "  Rule: ${RED}No assertions found${NC}"
      echo -e "  Fix: Add at least one assertion (assertEquals, assertTrue, assertThat, etc.)"
      VIOLATIONS=$((VIOLATIONS + 1))
    fi
    
    # Check anti-patterns
    anti_pattern_violations=$(check_anti_patterns "$file" "$line_num" "$method_end" "$method_name")
    if [ -n "$anti_pattern_violations" ]; then
      echo -e "${RED}✗ VIOLATION: $file${NC}"
      echo -e "  Method: ${YELLOW}$method_name${NC} (line $line_num)"
      echo -e "  Rule: ${RED}Anti-pattern detected${NC}"
      echo -e "$anti_pattern_violations"
      VIOLATIONS=$((VIOLATIONS + 1))
    fi
    
  done < <(extract_test_methods "$file")
done

# Summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test Structure Validation Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Files checked: $CHECKED_FILES"
echo "Test methods checked: $CHECKED_TESTS"

if [ $VIOLATIONS -eq 0 ]; then
  echo -e "${GREEN}✓ All tests pass validation${NC}"
  exit 0
else
  echo -e "${RED}✗ Found $VIOLATIONS violation(s)${NC}"
  exit 1
fi
