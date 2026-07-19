# Changelog

All notable changes to Spoke will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project uses a two-part `MAJOR.MINOR` version scheme.

## [1.0] - 2026-07-18

### Added
- **Core Features**
  - Real-time bike-share station information and availability
  - Trip tracking and active ride status monitoring
  - Trip history with duration, distance, and cost tracking
  - Station search and sorting (alphabetically or by proximity)
  - Multiple bike-share system support (see "Feature Support By System")
  - System switching with independent session management per system
  - Paid/unpaid fees status monitoring
  - Lifetime ride statistics and analytics
  - Customizable distance units (imperial/metric)
  - EXPERIMENTAL: Compass view with magnetic bearing integration to find a station (debug-only)

### Feature Support by System

| Feature | Indego | Metro Bike Share | RTC Bike Share |
|---------|--------|-----------------|----------------|
| Real-time station info | ✅ | ✅ | ✅ |
| Station search & filtering | ✅ | ✅ | ✅ |
| Bike availability status | ✅ | ✅ | ✅ |
| Classic/Electric bike tracking | ✅ | ✅ | ✅ |
| Bike checkout | ✅ | ⚠️ Partial | ❌ |
| Active trip tracking | ✅ | ⚠️ Partial | ❌ |
| Trip history | ✅ | ⚠️ Partial | ❌ |
| Lifetime statistics | ✅ | ⚠️ Partial | ❌ |
| Fees/pricing info | ✅ | ⚠️ Partial | ❌ |

**Legend:** ✅ = Fully supported | ⚠️ = Partial support | ❌ = Read-only or not supported

## Support

For issues, feature requests, or questions:
- Review [GitHub Issues](https://github.com/Snarr/Spoke/issues)
- See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
