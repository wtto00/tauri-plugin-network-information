//
//  NetworkInformationPlugin.swift
//  tauri-plugin-network-information
//
//  Created by wtto on 2024/10/29.
//

import CoreTelephony
import Network
import OSLog
import SwiftRs
import Tauri
import UIKit
import WebKit

let log = OSLog(subsystem: "com.tauri.dev", category: "plugin.network.information")

class IsOnloneArgs: Decodable {
  let hostname: String?
  let port: UInt16?
}

enum Connection: Int {
  case UNKNOWN
  case ETHERNET
  case WIFI
  case Cell2g
  case Cell3g
  case Cell4g
  case Cell5g
  case CELL
  case NONE
}

class NetworkInformationPlugin: Plugin {
  private let monitor = NWPathMonitor()
  private let queue = DispatchQueue.global(qos: .background)
  private let telephonyInfo = CTTelephonyNetworkInfo()

  private(set) var connectionType: Connection = .UNKNOWN

  override init() {
    super.init()
    monitor.pathUpdateHandler = { [self] path in
      updateConnectionType(path)
    }
    monitor.start(queue: queue)
    updateConnectionType(monitor.currentPath)
  }

  private func updateConnectionType(_ path: NWPath) {
    os_log(.debug, log: log, "path.status: %{public}@", "\(path.status)")
    try? trigger("available", data: path.status == .satisfied)
    if path.status != .satisfied {
      connectionType = .NONE
      return
    }
    if path.usesInterfaceType(.wifi) {
      connectionType = .WIFI
      return
    }
    if path.usesInterfaceType(.wiredEthernet) {
      connectionType = .ETHERNET
      return
    }
    if path.usesInterfaceType(.cellular) {
      guard let currentRadioTech = telephonyInfo.serviceCurrentRadioAccessTechnology?.values.first else {
        connectionType = .CELL
        return
      }
      os_log(.debug, log: log, "currentRadioTech: %{public}@", currentRadioTech.description)

      if #available(iOS 14.1, *) {
        if currentRadioTech == CTRadioAccessTechnologyNR || currentRadioTech == CTRadioAccessTechnologyNRNSA {
          connectionType = .Cell5g
          return
        }
      }

      switch currentRadioTech {
        case CTRadioAccessTechnologyGPRS, CTRadioAccessTechnologyEdge,
             CTRadioAccessTechnologyCDMA1x:
          connectionType = .Cell2g
        case CTRadioAccessTechnologyWCDMA,
             CTRadioAccessTechnologyHSDPA,
             CTRadioAccessTechnologyHSUPA,
             CTRadioAccessTechnologyCDMAEVDORev0,
             CTRadioAccessTechnologyCDMAEVDORevA,
             CTRadioAccessTechnologyCDMAEVDORevB,
             CTRadioAccessTechnologyeHRPD:
          connectionType = .Cell3g
        case CTRadioAccessTechnologyLTE:
          connectionType = .Cell4g
        default:
          connectionType = .CELL
      }
      return
    }
    connectionType = .UNKNOWN
  }

  @objc public func connection(_ invoke: Invoke) throws {
    invoke.resolve(connectionType.rawValue)
  }

  @objc public func isOnline(_ invoke: Invoke) throws {
    var args: JSObject
    do {
      args = try invoke.getArgs()
    } catch {
      args = JSObject()
    }
    let host = NWEndpoint.Host(args.getString("hostname") ?? "8.8.8.8")
    let port = NWEndpoint.Port(rawValue: UInt16(args.getInt("port") ?? 53))!

    let connection = NWConnection(host: host, port: port, using: .tcp)

    connection.stateUpdateHandler = { state in
      switch state {
        case .ready:
          invoke.resolve(true)
          connection.cancel()
        case .failed:
          invoke.resolve(false)
          connection.cancel()
        default:
          break
      }
    }

    connection.start(queue: .global())
    
    let timeoutInterval: TimeInterval = 1.5
    DispatchQueue.global().asyncAfter(deadline: .now() + timeoutInterval) {
      if connection.state != .ready && connection.state != .cancelled {
        invoke.resolve(false)
        connection.cancel()
      }
    }
  }
}

@_cdecl("init_plugin_network_information")
func initPlugin() -> Plugin {
  return NetworkInformationPlugin()
}
