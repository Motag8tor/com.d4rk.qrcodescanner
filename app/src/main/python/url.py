import requests, tldextract, whois, datetime

class URL:
    def __init__(self, id, address):
        self.ID = id
        self.address = address
        self.harmless = 0
        self.malicious = 0
        self.suspicious = 0
        self.reason = None

        self.domain = None
        self.headers = None
        self.creation_date = None
        self.downloadable = False

        self.result = []
    
# --------------------------------------------------------

    def generate_report(self):
        if self.reason:
            self.result.append(self.reason)

        if self.address.startswith("ftp://"):
            self.downloadable = True
        elif self.address.startswith("sftp://"):
            self.downloadable = True
        else:
            try:
                self.headers = requests.head(self.address).headers
            except:
                print(f'Unable to retrieve header.')
            
            if self.headers:
                if self.headers.get("Content-Disposition"):
                    self.downloadable = "attachment" in self.headers.get("Content-Disposition")
                elif self.headers.get("Content-Type"):
                    self.downloadable = "application/" in self.headers.get("Content-Type")

        try:
            tld = tldextract.extract(self.address)
            self.domain = tld.registered_domain
        except:
            print(f'Unable to retrieve the top level domain.')

        if self.domain:
            registered = None
            distance = None
            try:
                registered = whois.whois(self.domain)
            except:
                print(f'Unable to retrieve registration date')

            try:
                distance = datetime.datetime.now() - registered.creation_date[0]
            except:
                try:
                    distance = datetime.datetime.now() - registered.creation_date
                except:
                    print(f'Unable to calculate registration date')
            

            if distance:
                if distance.days > 31:
                    self.result.append("more")
                else:
                    self.result.append("less")
        
        if self.downloadable:
            self.result.append("downloadable")

        if self.malicious >= 1:
            self.result.append("malicious")
        elif self.suspicious >= 1:
            self.result.append("suspicious")
        else:
            self.result.append("harmless")

    def set_harmless(self, value):
        self.harmless = int(value)
    
    def set_malicious(self, value):
        self.malicious = int(value)
    
    def set_suspicious(self, value):
        self.suspicious = int(value)
    
    def set_reason(self, value):
        self.reason = value

# --------------------------------------------------------

    def get_ID(self):
        return self.ID

    def get_results(self):
        return self.result