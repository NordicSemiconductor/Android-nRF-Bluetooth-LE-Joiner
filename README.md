# nRF Bluetooth LE Joiner
nRF Bluetooth LE Joiner is an application that lets you add new IoT nodes to a network based on Bluetooth Smart. The application will configure the IoT node to establish a connection to an IPv6 enabled router via Bluetooth Smart. 

The application will initally scan for IoT nodes in range which contains the Node Configuration Service. Once connected to a node the user is allowed to add these IoT nodes to the network based on Bluetooth Smart by pressing the configure button. The application also has to pre-configure the wifi networks within the app in order to add the IoT Nodes. This can be done via scanning for wifi networks in range or can be added manually by typing the SSID and Passphrase.

Available on play store here https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrfblejoiner

## Node Configuration Service
The Node Configuration Service exposes information necessary to configure the node so it can be added to the network. This service is not a service defined by the Bluetooth SIG, but a proprietary service defined by Nordic Semiconductor to demonstrate commissioning of a node.
The Node Configuration GATT Service does not depend on any other services and can operate only on Bluetooth low energy as transport.
This service contains three characteristics

*Commissioning SSID - The UUID of the Commissioning SSID characteristic is 0x77A9 over proprietary base. The data received on this characteristic will serve as the manufacturer-specific data of the advertisement data the next time the node enters connectable mode in Joining mode. This characteristic must be written before Joining mode can be requested.The size of the packet written to the Commissioning SSID characteristic must be between 6 and 16 octets. Packets must be in little endian (LSB first) order.

*Commissioning Keys Store - The UUID of the Commissioning Keys Store characteristic is 0x77B9 over proprietary base. The data received on this characteristic will serve as the Passkey while establishing a secure connection to the router in Joining mode. This characteristic must be written before Joining mode can be requested. An array of length zero must be written to indicate the absence of OOB data. The size of the packet written to the Commissioning Keys Store characteristic must be between 0 and 8 octets. Packets must be in little endian (LSB first) order.

*Commissioning Control Point The UUID of the Commissionning Control Point characteristic is 0x77C9 over proprietary base. The Commissioning Control Point characteristic is used to control the state of the commissioned node. All commissioning procedures are requested by writing to this characteristic.

### Note:

*Android 4.3 or newer is required.

*Tested on Samsung S3 with Android 4.3 and on Nexus 5, 6 and 9 with lollipop and Marshmallow.

*Location Services need to be enabled for scanning from android 6.0 Marshmallow and in addition, runtime persmission ACCESS_COARSE_LOCATION is also required.
