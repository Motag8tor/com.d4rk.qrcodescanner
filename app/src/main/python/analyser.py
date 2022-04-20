import validators, requests, re
import apikey, url, wifi, file
from io import BytesIO

class Analyser:
    def __init__(self, text, raw):
        self.text = text
        self.raw = raw

        self.file_class = None
        self.wifi_class = None
        self.url_class = None
        self.hexdump = bytes(raw).hex(" ")

    def get_hexdump(self):
        return self.hexdump

# --------------------------------------------------------
    
    def get_url_analysis(self):
        headers = {"Accept": "application/json",
			       "x-apikey": apikey.apikey()}

        if self.url_class: # Check if a url has been scanned
            id = self.url_class.get_ID()
        else:
            return 3 # If not then exit

        VT_url = "https://www.virustotal.com/api/v3/analyses/" + id
	
        try:
            response = requests.request("GET", VT_url, headers=headers)
        except requests.ConnectTimeout as timeout:
            print(timeout)
            return 1

        if response:
            data = response.json()
            status = data["data"]["attributes"]["status"]
		
        if status == "completed":
            print(data["data"]["attributes"]["stats"])
            self.url_class.set_harmless(data["data"]["attributes"]["stats"]["harmless"])
            self.url_class.set_malicious(data["data"]["attributes"]["stats"]["malicious"])
            self.url_class.set_suspicious(data["data"]["attributes"]["stats"]["suspicious"])
            self.url_class.generate_report() # Generate report
            return self.url_class.get_results() # Return the report on success
        elif status == "queued":
            return 2
        else:
            return 3

    def upload_url_for_scanning(self, address):
        headers = {"Accept": "application/json", 
			       "Content-Type": "application/x-www-form-urlencoded",
			       "x-apikey": apikey.apikey()}

        VT_url = "https://www.virustotal.com/api/v3/urls"

        try:
            response = requests.request("POST", VT_url, data="url=" + address, headers=headers)
        except requests.ConnectTimeout as timeout:
            print(timeout)
            return "Unable to submit URL for analysis. Try again."

        if response:
            data = response.json()
            report_id = data["data"]["id"]
            self.url_class = url.URL(report_id, address)
            return "url"
    
# --------------------------------------------------------
    
    def get_wifi_analysis(self):
        return self.wifi_class.get_report()

    def wifi_scanner(self, data):
        self.wifi_class = wifi.Wifi()

        array = re.findall("(.+?):((?:[^\\;]|\\.)*);", data[5:])
        print(array)

        for i in array:
            if i[0] == "S":
                self.wifi_class.set_SSID(i[1])
            elif i[0] == "T":
                self.wifi_class.set_authentication(i[1])
            elif i[0] == "P":
                self.wifi_class.set_password(i[1])
            elif i[0] == "H":
                self.wifi_class.set_hidden()

# --------------------------------------------------------

    def get_file_analysis(self):
        if self.file_class: # Check if a file has been scanned
            id = self.file_class.get_ID()
            print(id)
        else:
            return 3 # If not then exit

        headers = {"Accept": "application/json",
                   "x-apikey": apikey.apikey()}

        VT_url = "https://www.virustotal.com/api/v3/files/" + id
	
        try:
            response = requests.request("GET", VT_url, headers=headers)
        except requests.ConnectTimeout as timeout:
            print(timeout)
            return 1

        if response:
            data = response.json()
            status = data["data"]["attributes"]["last_analysis_results"]
		
        if status:
            print(data["data"]["attributes"]["last_analysis_stats"])
            self.file_class.set_harmless(data["data"]["attributes"]["last_analysis_stats"]["harmless"])
            self.file_class.set_malicious(data["data"]["attributes"]["last_analysis_stats"]["malicious"])
            self.file_class.set_suspicious(data["data"]["attributes"]["last_analysis_stats"]["suspicious"])
            return self.file_class.get_report() # Generate and return report
        elif not status:
            return 2
        else:
            return 3

    def upload_file_for_scanning(self, contents):
        headers = {"x-apikey": apikey.apikey()}

        VT_url = 'https://www.virustotal.com/api/v3/files'

        data_file = BytesIO(bytes(contents))
        print(data_file.read())
        data_file.seek(0)
        files = {'file': ('file.exe', data_file)}

        try:
            response = requests.post(VT_url, headers=headers, files=files)
        except requests.ConnectTimeout as timeout:
            print(timeout)
            return "Unable to submit file for analysis. Try again."

        if response:
            data = response.json()
            report_id = data["data"]["id"]
            self.file_class = file.File(report_id)
            return "file"

# --------------------------------------------------------
    
    def analyser(self):
        print(f'The QR Code contains: {self.text}')
        print(f'The raw data is: {self.raw}')
        print(f'Hexdump:\n{self.hexdump}')

        valid_url = validators.url(self.text)
        if valid_url:
            print(f'URL Found...')
            self.url_class = None
            return self.upload_url_for_scanning(self.text)

        valid_wifi = re.search("^WIFI:((?:.+?:(?:[^\\;]|\\.)*;)+);?$", self.text)
        if valid_wifi:
            print(f'Wi-Fi Network Found...')
            self.wifi_class = None
            self.wifi_scanner(self.text)
            return "wifi"

        if not valid_url or not valid_wifi:
            print(f'Generic file upload')
            self.file_class = None
            return self.upload_file_for_scanning(self.raw)

        return 0




#if __name__ == "__main__":
#    a = Analyser()
#    a.analyser("https://google.com")
