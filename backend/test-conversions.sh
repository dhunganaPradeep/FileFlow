#!/bin/bash

# Configuration
API_URL="http://localhost:8080/api"
TEST_DATA_DIR="test-data"

# Ensure test data directory exists
mkdir -p "$TEST_DATA_DIR"

# Create test files if they don't exist
create_test_file() {
    local file=$1
    local content=$2
    if [ ! -f "$TEST_DATA_DIR/$file" ]; then
        echo "$content" > "$TEST_DATA_DIR/$file"
        echo "Created $file"
    fi
}

create_test_file "sample.txt" "This is a sample text file for conversion testing."
if [ ! -f "$TEST_DATA_DIR/sample.jpg" ]; then
    convert -size 100x100 xc:white "$TEST_DATA_DIR/sample.jpg"
fi
if [ ! -f "$TEST_DATA_DIR/sample.png" ]; then
    convert -size 100x100 xc:blue "$TEST_DATA_DIR/sample.png"
fi
if [ ! -f "$TEST_DATA_DIR/sample.mp3" ]; then
    ffmpeg -f lavfi -i anullsrc=r=44100:cl=mono -t 1 -y "$TEST_DATA_DIR/sample.mp3" 2>/dev/null
fi
if [ ! -f "$TEST_DATA_DIR/sample.mp4" ]; then
    ffmpeg -f lavfi -i testsrc=duration=1:size=160x120:rate=10 -f lavfi -i anullsrc=r=44100:cl=mono -t 1 -y "$TEST_DATA_DIR/sample.mp4" 2>/dev/null
fi
if [ ! -f "$TEST_DATA_DIR/sample.pdf" ]; then
    echo "Sample PDF Content" | pandoc -o "$TEST_DATA_DIR/sample.pdf"
fi

echo "=== Fileflow Automated Integration Tests ==="

test_conversion() {
    local input_file=$1
    local target_format=$2
    local mime_type=$3
    
    echo "Testing: $input_file -> $target_format ($mime_type)"
    
    # Upload
    local upload_resp=$(curl -s -X POST "$API_URL/upload?targetFormat=$target_format" \
        -H "Content-Type: multipart/form-data" \
        -F "file=@$TEST_DATA_DIR/$input_file;type=$mime_type")
    
    local job_id=$(echo $upload_resp | jq -r '.jobId')
    local token=$(echo $upload_resp | jq -r '.token')
    
    if [ "$job_id" == "null" ] || [ -z "$job_id" ]; then
        echo "  FAILED: Upload failed - $(echo $upload_resp | jq -r '.message')"
        return 1
    fi
    
    # Poll
    local max_attempts=20
    local attempt=0
    while [ $attempt -lt $max_attempts ]; do
        local status_resp=$(curl -s "$API_URL/jobs/$job_id?token=$token")
        local status=$(echo $status_resp | jq -r '.status')
        
        if [ "$status" == "COMPLETED" ]; then
            echo "  SUCCESS: Conversion completed!"
            return 0
        elif [ "$status" == "FAILED" ]; then
            echo "  FAILED: $(echo $status_resp | jq -r '.errorMessage')"
            return 1
        fi
        
        attempt=$((attempt + 1))
        sleep 1
    done
    
    echo "  FAILED: Timeout"
    return 1
}

# Test Cases
test_conversion "sample.jpg" "png" "image/jpeg"
test_conversion "sample.png" "webp" "image/png"
test_conversion "sample.png" "pdf" "image/png"
test_conversion "sample.txt" "pdf" "text/plain"
test_conversion "sample.pdf" "docx" "application/pdf"
test_conversion "sample.mp3" "wav" "audio/mpeg"
test_conversion "sample.mp4" "mp3" "video/mp4"
test_conversion "sample.mp4" "webm" "video/mp4"

# Simulated user reported case
echo "Dummy content" > "$TEST_DATA_DIR/dummy.docx"
test_conversion "dummy.docx" "pdf" "application/x-tika-ooxml"
rm "$TEST_DATA_DIR/dummy.docx"

echo "All tests finished!"
