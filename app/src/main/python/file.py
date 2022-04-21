import base64

class File:
    def __init__(self, id):
        if "-" not in id:   
            decoded = base64.b64decode(id).decode()
            self.ID = decoded.split(":")[0]
        else:
            self.ID = id.split("-")[0]
        print(f'The ID is {self.ID}')
        self.harmless = 0
        self.malicious = 0
        self.suspicious = 0
        self.reason = None
        self.result = []
    
    def get_ID(self):
        return self.ID

    # --------------------------------------------------------

    def set_harmless(self, value):
        self.harmless = int(value)
    
    def set_malicious(self, value):
        self.malicious = int(value)
    
    def set_suspicious(self, value):
        self.suspicious = int(value)
    
    def set_reason(self, value):
        self.reason = value

    # --------------------------------------------------------

    def get_report(self):
        if self.reason:
            self.result.append(self.reason)

        if self.malicious >= 1:
            self.result.append("malicious")
        elif self.suspicious >= 1:
            self.result.append("suspicious")
        else:
            self.result.append("harmless")
        return self.result