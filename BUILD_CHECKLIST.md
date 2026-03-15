# ⚡ Quick Build Performance Checklist

## ✅ Phase 1 Fixes Applied (2026-02-01)

### Critical Fixes
- [x] **KSP Incremental:** `ksp.incremental=true` (was: false)
- [x] **Gradle Heap:** 6GB (was: 2GB)
- [x] **Parallel Builds:** `org.gradle.parallel=true`
- [x] **Build Cache:** `org.gradle.caching=true`
- [x] **Kotlin Incremental:** `kotlin.incremental=true`
- [x] **Jetifier:** Disabled (was causing 15-20s overhead)
- [x] **Legacy Library:** Removed `com.android.support:design:28.0.0`
- [x] **Compose Metrics:** Enabled (reports in `app/build/compose_metrics/`)
- [x] **Kotlin Version:** Updated to 1.9.22 (was: 1.9.10)
- [x] **Compose Compiler:** Updated to 1.5.10 (was: 1.5.3)
- [x] **DataBinding:** Disabled (unused)

---

## 🧪 Test Your Improvements

### 1. Clean Build Test
```bash
./gradlew --stop
./gradlew clean assembleDebug --profile --scan
```
**Expected:** 3-5 minutes (was: 5-8 minutes)

### 2. Incremental Build Test
```bash
# Make a small change, then:
./gradlew assembleDebug --profile
```
**Expected:** 10-20 seconds (was: 60-120 seconds)

### 3. Check Compose Metrics
```bash
# After build, check:
app/build/compose_metrics/
```
Look for unstable/non-skippable composables

---

## 🎯 Next Steps (Priority Order)

### Priority 1 (This Week)
- [ ] Run build tests above
- [ ] Analyze Compose metrics
- [ ] Migrate to Compose BOM
- [ ] Remove duplicate coroutines dependency

### Priority 2 (Next Week)
- [ ] Update AGP to 8.7.3
- [ ] Profile app startup
- [ ] Optimize Application class

### Priority 3 (Next Sprint)
- [ ] Plan modularization
- [ ] Create `:core:data`, `:core:ui` modules
- [ ] Migrate features to modules

---

## 🚨 Known Issues

### 16 KB Page Size
**Status:** ❌ NOT APPLICABLE  
**Reason:** No custom native code in app

### First Build After Changes
**Status:** ⚠️ EXPECTED SLOW (5-10 min)  
**Reason:** Gradle downloads new versions, invalidates cache

---

## 📊 Performance Targets

| Metric | Target | Red Flag |
|--------|--------|----------|
| Clean build | < 5 min | > 8 min |
| Incremental build | < 20s | > 30s |
| Gradle daemon | Stable | Crashes |
| Build cache hit rate | > 50% | < 30% |

---

## 🛠️ Quick Commands

```bash
# Stop Gradle daemon
./gradlew --stop

# Build with profiling
./gradlew assembleDebug --profile --scan

# Check Gradle version
./gradlew --version

# List all tasks
./gradlew tasks --all

# Check dependencies
./gradlew :app:dependencies

# Analyze build
# Android Studio: Build → Analyze Build
```

---

**Last Updated:** 2026-02-01  
**Status:** Phase 1 Complete ✅
