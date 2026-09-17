# Face Recognition Benchmark Plan
## AFORA - Smart Classroom Attendance Management

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Benchmark Methodology Definition

---

## 1. Benchmark Objectives

### 1.1 Goals

1. **Select optimal detection model** for multi-face classroom scenarios
2. **Select optimal recognition model** for mobile deployment
3. **Determine confidence thresholds** based on actual performance
4. **Validate feasibility** of on-device face recognition
5. **Measure real-world performance** in classroom conditions
6. **Establish performance baselines** for production monitoring

### 1.2 Non-Goals

- ❌ Train custom model (use existing models)
- ❌ Achieve 100% accuracy (impossible, unrealistic)
- ❌ Select model based on popularity (benchmark required)
- ❌ Hard-code thresholds before testing

---

## 2. Candidate Models

### 2.1 Detection Models

| Model | Source | Key Features |
|-------|--------|--------------|
| **MTCNN** | Multi-task Cascaded CNN | 3-stage cascade, landmark detection, slower but accurate |
| **BlazeFace** | Google MediaPipe | Lightweight, very fast, mobile-optimized |
| **MediaPipe Face Detection** | Google | Fast, good tracking, well-maintained |
| **YuNet** | OpenCV | Balanced speed/accuracy, newer |

### 2.2 Recognition Models

| Model | Embedding Size | Key Features |
|-------|----------------|--------------|
| **FaceNet** | 128/512 | Industry standard, good accuracy, larger model |
| **MobileFaceNet** | 128 | Mobile-optimized, faster, slightly lower accuracy |
| **ArcFace** | 512 | State-of-art accuracy, heavy computation |
| **InsightFace** | 512 | Good balance, widely used |

### 2.3 Model Combinations

**Test Matrix**:
```
Detection Model × Recognition Model = 4 × 4 = 16 combinations

Examples:
1. MTCNN + FaceNet
2. MTCNN + MobileFaceNet
3. BlazeFace + FaceNet
4. BlazeFace + MobileFaceNet
5. MediaPipe + FaceNet
6. MediaPipe + MobileFaceNet
...
```

**Prioritization**:
- Start with most promising combinations (e.g., BlazeFace + MobileFaceNet)
- Eliminate poorly performing combinations early
- Focus on top 3-5 combinations for detailed testing

---

## 3. Benchmark Dataset

### 3.1 Data Collection Requirements

**Minimum Dataset Size**:
- **100-200 unique individuals**
- **3-5 images per person** (enrollment simulation)
- **1000+ test images** (various conditions)

**Collection Method**:
1. Recruit volunteers (students, staff)
2. Obtain written consent
3. Capture images in controlled environment
4. Capture images in classroom-like environment
5. Anonymize data (assign ID, remove metadata)

### 3.2 Image Capture Conditions

**Enrollment Images** (3-5 per person):
- Frontal face
- Slight left (±15°)
- Slight right (±15°)
- Good lighting
- Neutral expression
- No occlusion
- Resolution: 640x480 minimum

**Test Images** (diverse conditions):

| Condition | Count per Person | Purpose |
|-----------|------------------|---------|
| Frontal, good lighting | 2-3 | Baseline accuracy |
| Left profile (30°) | 1-2 | Angle robustness |
| Right profile (30°) | 1-2 | Angle robustness |
| Low lighting | 1-2 | Lighting robustness |
| High lighting (backlit) | 1-2 | Lighting robustness |
| Glasses | 1-2 | Occlusion handling |
| Mask (lower face) | 1-2 | Occlusion handling |
| 2m distance | 1-2 | Distance robustness |
| 5m distance | 1-2 | Distance robustness |
| 10m distance | 1-2 | Distance robustness |
| Group photo (5-10 people) | 3-5 | Multi-face detection |
| Group photo (20-30 people) | 3-5 | High-density detection |
| Classroom simulation | 5-10 | Real-world scenario |

**Total per person**: ~25-40 test images

### 3.3 Data Organization

```
benchmark_dataset/
├── enrollment/
│   ├── person_001/
│   │   ├── front.jpg
│   │   ├── left_15.jpg
│   │   ├── right_15.jpg
│   │   ├── front_2.jpg
│   │   └── metadata.json
│   ├── person_002/
│   └── ...
├── test/
│   ├── person_001/
│   │   ├── frontal_good_light_1.jpg
│   │   ├── frontal_good_light_2.jpg
│   │   ├── left_30_1.jpg
│   │   ├── low_light_1.jpg
│   │   ├── glasses_1.jpg
│   │   └── metadata.json
│   ├── person_002/
│   └── ...
├── multi_face/
│   ├── group_5_people_1.jpg
│   ├── group_10_people_1.jpg
│   ├── group_20_people_1.jpg
│   ├── classroom_sim_1.jpg
│   └── annotations.json (bounding boxes)
└── unknowns/
    ├── unknown_001.jpg (not in enrollment set)
    ├── unknown_002.jpg
    └── ...
```

---

## 4. Evaluation Metrics

### 4.1 Detection Metrics

| Metric | Formula | Target | Notes |
|--------|---------|--------|-------|
| **Detection Rate** | Detected Faces / Total Faces | > 95% | Baseline: frontal, good lighting |
| **False Positive Rate** | False Detections / Total Detections | < 5% | Non-face detected as face |
| **Multi-Face Capacity** | Max faces detected correctly | ≥ 30 | Typical classroom size |
| **Average Precision (AP)** | IoU-based metric | > 0.85 | Standard CV metric |

### 4.2 Recognition Metrics

| Metric | Formula | Target | Notes |
|--------|---------|--------|-------|
| **True Accept Rate @ 1% FAR** | TAR @ FAR=1% | > 90% | Industry standard |
| **True Accept Rate @ 0.1% FAR** | TAR @ FAR=0.1% | > 85% | High security |
| **False Accept Rate** | False Accepts / Total Comparisons | < 1% | Wrong person accepted |
| **False Reject Rate** | False Rejects / Total Genuine | < 10% | Correct person rejected |
| **Rank-1 Accuracy** | Correct match in top 1 | > 90% | Correct ID is top match |
| **Rank-5 Accuracy** | Correct match in top 5 | > 95% | Correct ID in top 5 |

### 4.3 Performance Metrics

| Metric | Target | Max Acceptable | Notes |
|--------|--------|----------------|-------|
| **Detection Time (per frame)** | < 200ms | < 500ms | Time to detect all faces |
| **Embedding Time (per face)** | < 100ms | < 300ms | Time to generate embedding |
| **Matching Time (per face)** | < 50ms | < 100ms | Time to match against enrolled set |
| **End-to-End Latency** | < 300ms | < 800ms | Total time per frame |
| **FPS (Frames Per Second)** | 2-3 fps | 1 fps minimum | Acceptable for classroom scanning |
| **Memory Usage (Peak)** | < 300MB | < 500MB | Total app memory |
| **Battery Drain** | < 15%/10min | < 30%/10min | Power consumption |
| **Thermal Throttling** | < 5°C increase | < 10°C increase | Device temperature |

### 4.4 Robustness Metrics

| Condition | Target Accuracy | Notes |
|-----------|-----------------|-------|
| Frontal, good lighting | > 95% | Baseline |
| ±30° angle | > 85% | Profile |
| Low lighting | > 80% | Dim classroom |
| High lighting (backlit) | > 75% | Window glare |
| Glasses | > 85% | Common occlusion |
| Mask (lower face covered) | > 60% | Partial occlusion |
| 2m distance | > 90% | Near |
| 5m distance | > 85% | Mid |
| 10m distance | > 70% | Far (back of classroom) |

---

## 5. Test Scenarios

### 5.1 Controlled Environment Tests

**Scenario 1: Single Face, Frontal, Good Lighting**
- **Purpose**: Establish baseline accuracy
- **Setup**: Single person, frontal view, controlled lighting
- **Metrics**: Detection rate, recognition accuracy, latency
- **Expected**: > 95% accuracy

**Scenario 2: Single Face, Various Angles**
- **Purpose**: Test angle robustness
- **Setup**: Same person at ±15°, ±30°, ±45°
- **Metrics**: Recognition accuracy degradation
- **Expected**: Gradual degradation, > 85% at ±30°

**Scenario 3: Single Face, Lighting Variations**
- **Purpose**: Test lighting robustness
- **Setup**: Bright, normal, dim, backlit
- **Metrics**: Recognition accuracy per condition
- **Expected**: > 80% in most lighting

**Scenario 4: Single Face, Occlusions**
- **Purpose**: Test occlusion handling
- **Setup**: Glasses, hat, mask (lower face)
- **Metrics**: Recognition accuracy with occlusion
- **Expected**: > 85% with glasses, > 60% with mask

**Scenario 5: Unknown Faces**
- **Purpose**: Test false accept rate
- **Setup**: Faces NOT in enrollment set
- **Metrics**: How many unknowns incorrectly matched
- **Expected**: < 1% false accepts

### 5.2 Multi-Face Tests

**Scenario 6: Group Photo (5-10 People)**
- **Purpose**: Test multi-face detection
- **Setup**: Group photo, all faces enrolled
- **Metrics**: Detection count, recognition accuracy
- **Expected**: Detect all faces, > 90% recognized

**Scenario 7: Group Photo (20-30 People)**
- **Purpose**: Test high-density detection
- **Setup**: Large group, classroom-like
- **Metrics**: Detection count, recognition accuracy, latency
- **Expected**: Detect ≥ 25 faces, > 85% recognized

**Scenario 8: Classroom Simulation**
- **Purpose**: Simulate real classroom scanning
- **Setup**: 30-40 people in classroom, teacher scans with phone
- **Metrics**: Total detected after scanning, accuracy, time taken
- **Expected**: Detect ≥ 80% of class, < 5 minutes total

### 5.3 Cross-Frame Deduplication Tests

**Scenario 9: Same Person in Multiple Frames**
- **Purpose**: Test deduplication logic
- **Setup**: Capture same person in 5 consecutive frames
- **Metrics**: Final unique count (should be 1)
- **Expected**: 100% deduplication success

**Scenario 10: Multiple People Across Frames**
- **Purpose**: Test deduplication with movement
- **Setup**: 10 people, scan left-to-right, people move between frames
- **Metrics**: Final unique count (should be 10)
- **Expected**: 100% deduplication, no duplicates

### 5.4 Performance Stress Tests

**Scenario 11: Continuous Scanning (10 Minutes)**
- **Purpose**: Test sustained performance, thermal behavior
- **Setup**: Continuous face detection/recognition for 10 minutes
- **Metrics**: Battery drain, temperature increase, memory leaks, performance degradation
- **Expected**: < 15% battery drain, < 5°C temp increase, stable performance

**Scenario 12: Large Enrollment Set**
- **Purpose**: Test recognition performance with realistic enrollment size
- **Setup**: Enrollment set of 60 students (typical class size)
- **Metrics**: Recognition latency, accuracy
- **Expected**: < 100ms matching time, no accuracy loss

---

## 6. Benchmark Procedure

### 6.1 Phase 1: Initial Screening (Week 1)

**Objective**: Eliminate poorly performing models quickly

**Steps**:
1. Implement all detection models
2. Implement all recognition models
3. Run Scenario 1 (baseline) for all 16 combinations
4. Measure: accuracy, latency, memory
5. **Eliminate**: Combinations with < 90% baseline accuracy OR > 1s latency
6. **Shortlist**: Top 5 combinations for detailed testing

**Deliverable**: Shortlist of 3-5 model combinations

---

### 6.2 Phase 2: Detailed Testing (Week 2)

**Objective**: Comprehensive evaluation of shortlisted models

**Steps**:
1. Run all scenarios (1-12) for each shortlisted combination
2. Collect detailed metrics
3. Analyze robustness (angles, lighting, occlusions, distance)
4. Analyze multi-face performance
5. Analyze performance metrics (latency, memory, battery, thermal)

**Deliverable**: Detailed benchmark report with metrics table

---

### 6.3 Phase 3: Confidence Threshold Tuning (Week 2-3)

**Objective**: Determine optimal confidence thresholds

**Method**:
1. For selected model, collect raw similarity scores for all test images
2. Plot True Accept Rate (TAR) vs False Accept Rate (FAR) curve (ROC curve)
3. Identify operating points:
   - **High Confidence**: FAR = 0.1% → TAR = ?% → Threshold = ?
   - **Medium Confidence**: FAR = 1% → TAR = ?% → Threshold = ?
   - **Low Confidence**: FAR = 5% → TAR = ?% → Threshold = ?
4. Balance false accepts vs false rejects based on use case
5. **Decision**: Should low-confidence matches go to teacher review? (Yes)

**Deliverable**: Confidence threshold recommendations with justification

---

### 6.4 Phase 4: Real-World Pilot (Week 3)

**Objective**: Validate in actual classroom conditions

**Setup**:
1. Select 2-3 pilot classes (small, controlled)
2. Enroll students (face registration)
3. Teachers take attendance using selected model
4. Collect real-world metrics:
   - Time to complete attendance
   - Recognition accuracy (compared to manual ground truth)
   - Teacher correction rate
   - Unknown face rate
   - Teacher satisfaction feedback

**Deliverable**: Pilot test report, final model recommendation

---

## 7. Benchmark Environment

### 7.1 Hardware

**Test Devices** (Android):
- **High-End**: Flagship phone (e.g., Samsung Galaxy S23, Pixel 7)
- **Mid-Range**: Mid-tier phone (e.g., Samsung A-series, OnePlus Nord)
- **Budget**: Entry-level phone (minimum supported hardware)

**Rationale**: Performance varies significantly across device tiers

### 7.2 Software

- Android SDK: 24+ (Android 7.0+)
- TensorFlow Lite: Latest stable
- ONNX Runtime: Latest stable (if using ONNX models)
- Camera: CameraX library
- Test Framework: JUnit 5, Espresso

### 7.3 Benchmark App

**Create dedicated benchmark app**:
```
BenchmarkApp/
├── ModelLoader (load all models)
├── DetectionBenchmark (test detection models)
├── RecognitionBenchmark (test recognition models)
├── EndToEndBenchmark (full pipeline test)
├── PerformanceMonitor (memory, battery, thermal)
├── MetricsCollector (save results to CSV/JSON)
└── ReportGenerator (generate benchmark report)
```

**Features**:
- Load dataset from device storage
- Run automated tests
- Collect detailed metrics
- Export results for analysis

---

## 8. Decision Criteria

### 8.1 Must-Have Requirements (Mandatory)

- ✅ Baseline accuracy > 90%
- ✅ Multi-face detection (≥ 20 faces)
- ✅ Latency < 500ms per frame
- ✅ Memory < 500MB
- ✅ False Accept Rate < 1%
- ✅ Android compatibility
- ✅ Portable to iOS (future)

### 8.2 Trade-Off Analysis

| Priority | Metric | Weight |
|----------|--------|--------|
| 1 | Recognition Accuracy | 35% |
| 2 | Latency (Speed) | 25% |
| 3 | Multi-Face Handling | 20% |
| 4 | Memory Usage | 10% |
| 5 | Battery/Thermal | 10% |

**Scoring Formula**:
```
Score = (0.35 × Accuracy) + (0.25 × Speed) + (0.20 × MultiFace) + 
        (0.10 × Memory) + (0.10 × Battery)

Normalize each metric to 0-100 scale before applying weights.
```

**Selection**: Highest score wins

---

## 9. Deliverables

### 9.1 Benchmark Report

**Template**:

```markdown
# Face Recognition Benchmark Report

## Executive Summary
- Models tested: [list]
- Dataset size: [X images, Y people]
- Recommended model: [Detection + Recognition]
- Key findings: [bullet points]

## Detection Model Results

| Model | Detection Rate | FPS | Memory | Multi-Face (30) | Score |
|-------|----------------|-----|--------|-----------------|-------|
| MTCNN | 96% | 1.2 fps | 280MB | 28/30 | 82 |
| BlazeFace | 94% | 3.5 fps | 150MB | 30/30 | 91 |
| ... | ... | ... | ... | ... | ... |

## Recognition Model Results

| Model | TAR @ 1% FAR | Latency | Memory | Angle Robust | Distance Robust | Score |
|-------|--------------|---------|--------|--------------|-----------------|-------|
| FaceNet | 92% | 120ms | 450MB | 87% | 82% | 88 |
| MobileFaceNet | 89% | 80ms | 220MB | 84% | 79% | 91 |
| ... | ... | ... | ... | ... | ... | ... |

## Recommended Combination

**Detection**: [Model Name]  
**Recognition**: [Model Name]  
**Overall Score**: [X/100]

**Rationale**: [Why this combination]

## Confidence Thresholds

| Category | Threshold | TAR | FAR | Decision |
|----------|-----------|-----|-----|----------|
| HIGH | ≥ 0.87 | 91% | 0.1% | Auto-accept |
| MEDIUM | 0.75-0.86 | 88% | 1% | Teacher review |
| LOW | 0.60-0.74 | 82% | 3% | Teacher review |
| VERY_LOW | 0.50-0.59 | 71% | 8% | Teacher review |
| UNKNOWN | < 0.50 | - | - | Unknown face |

## Real-World Pilot Results

- Classes tested: [X]
- Total attendance sessions: [Y]
- Average time per session: [Z minutes]
- Recognition accuracy: [%]
- Teacher correction rate: [%]
- Teacher satisfaction: [rating]

## Recommendations

1. Use [Detection Model] + [Recognition Model]
2. Set confidence thresholds as above
3. Implement teacher review for MEDIUM/LOW confidence
4. Consider [specific optimizations]

## Known Limitations

- [Limitation 1]
- [Limitation 2]
```

### 9.2 Updated Documentation

**Update the following docs**:
- `face-recognition.md`: Add selected model, thresholds
- `decision-log.md`: Mark FR-001, FR-002 as DECIDED
- `architecture.md`: Update CV pipeline with specific model names

---

## 10. Success Criteria

**Benchmark is successful if**:

✅ Selected model achieves:
- > 90% recognition accuracy (baseline)
- < 500ms per frame latency
- ≥ 20 faces detected in multi-face scenario
- < 500MB memory usage
- < 15% battery drain per 10 minutes

✅ Confidence thresholds determined empirically

✅ Real-world pilot validates performance

✅ Teacher feedback is positive (usable in classroom)

**Benchmark fails if**:

❌ No model meets minimum requirements  
❌ Performance too slow for practical use  
❌ Memory/battery consumption too high  
❌ Pilot test shows high teacher correction rate (> 20%)

**Contingency if benchmark fails**:
1. Re-evaluate model selection (try other models)
2. Consider cloud-based recognition (trade latency for accuracy)
3. Consider reducing accuracy target (if acceptable)
4. Rely more heavily on manual fallback

---

## 11. Timeline

**Total Duration**: 3-4 weeks (Phase 5 in roadmap)

| Week | Activities | Deliverable |
|------|------------|-------------|
| Week 1 | Initial screening, shortlist models | Top 3-5 model combinations |
| Week 2 | Detailed testing, robustness evaluation | Detailed benchmark report |
| Week 2-3 | Confidence threshold tuning | Threshold recommendations |
| Week 3 | Real-world pilot testing | Pilot test report |
| Week 3-4 | Analysis, documentation, decision | Final model selection + updated docs |

---

## 12. Open Decisions

| Decision | Status | Notes |
|----------|--------|-------|
| Detection model | 🧪 BENCHMARK-DEPENDENT | Phase 5 |
| Recognition model | 🧪 BENCHMARK-DEPENDENT | Phase 5 |
| Confidence thresholds | 🧪 BENCHMARK-DEPENDENT | Phase 5 |
| Liveness detection | 🔮 DEFERRED | Evaluate if spoofing detected in pilot |
| Model quantization | 📝 ASSUMPTION | Try if performance insufficient |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | ML Engineer | Initial benchmark plan |

**Status**: Methodology Defined  
**Next**: Execute in Phase 5 (CV Technical Spike)
